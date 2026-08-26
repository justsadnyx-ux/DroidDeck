package ux.justsadnyx.droiddeck;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class Util {

    private Util() {}

    public static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb);
        double gb = mb / 1024.0;
        return String.format(Locale.US, "%.2f GB", gb);
    }

    public static String humanDuration(long millis) {
        long s = millis / 1000;
        long d = s / 86400; s %= 86400;
        long h = s / 3600; s %= 3600;
        long m = s / 60; s %= 60;
        if (d > 0) return String.format(Locale.US, "%dd %dh %dm", d, h, m);
        if (h > 0) return String.format(Locale.US, "%dh %dm", h, m);
        return String.format(Locale.US, "%dm %ds", m, s);
    }

    public static long totalRam(Context ctx) {
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.totalMem;
    }

    public static long availRam(Context ctx) {
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.availMem;
    }

    public static int batteryLevel(Context ctx) {
        BatteryManager bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    public static float batteryTemp(Context ctx) {
        Intent intent = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null) return 0f;
        int tenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
        return tenths / 10f;
    }

    public static long storageTotal() {
        StatFs stat = new StatFs(Environment.getDataDirectory().getAbsolutePath());
        return stat.getTotalBytes();
    }

    public static long storageFree() {
        StatFs stat = new StatFs(Environment.getDataDirectory().getAbsolutePath());
        return stat.getAvailableBytes();
    }

    public static String cpuInfo() {
        String hardware = Build.HARDWARE == null ? "" : Build.HARDWARE;
        int cores = Runtime.getRuntime().availableProcessors();
        String abi = Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "?";
        return cores + " cores · " + abi + (hardware.isEmpty() ? "" : "\n" + hardware);
    }

    public static int appCount(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            List<android.content.pm.PackageInfo> packages = pm.getInstalledPackages(0);
            return packages.size();
        } catch (Exception e) {
            return 0;
        }
    }

    public static String localIpAddresses() {
        StringBuilder sb = new StringBuilder();
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.isUp() || nif.isLoopback()) continue;
                for (InetAddress addr : Collections.list(nif.getInetAddresses())) {
                    if (addr.isSiteLocalAddress()) {
                        if (sb.length() > 0) sb.append("\n");
                        sb.append(addr.getHostAddress()).append("  (").append(nif.getName()).append(")");
                    }
                }
            }
        } catch (Exception ignored) {}
        return sb.length() == 0 ? "No local IP found" : sb.toString();
    }

    public static String wifiSsid(Context ctx) {
        WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm == null) return "unknown";
        android.net.wifi.WifiInfo info = wm.getConnectionInfo();
        if (info == null) return "unknown";
        String ssid = info.getSSID();
        if (ssid == null) return "unknown";
        ssid = ssid.replace("\"", "");
        return "<unknown ssid>".equals(ssid) ? "(not connected)" : ssid;
    }

    public interface TextCallback { void onText(String text); }

    public static void fetchPublicIp(TextCallback callback) {
        new Thread(() -> {
            String ip = fetchUrl("https://api.ipify.org");
            postResult(callback, ip == null ? "unavailable" : ip);
        }).start();
    }

    public static String fetchUrl(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "DroidDeck-Android");
            BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static void postResult(TextCallback cb, String value) {
        android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
        h.post(() -> cb.onText(value));
    }

    public static boolean isDarkMode(Context ctx) {
        int mode = ctx.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    public static String deviceReport(Context ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("== DroidDeck Device Report ==\n");
        sb.append("Model: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        sb.append("Android: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("Security patch: ").append(Build.VERSION.SECURITY_PATCH != null ? Build.VERSION.SECURITY_PATCH : "?").append("\n");
        sb.append("CPU cores: ").append(Runtime.getRuntime().availableProcessors()).append("\n");
        String abi = Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "?";
        sb.append("ABI: ").append(abi).append("\n");
        sb.append("Hardware: ").append(Build.HARDWARE != null ? Build.HARDWARE : "?").append("\n");

        android.util.DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        sb.append("Screen: ").append(dm.widthPixels).append("x").append(dm.heightPixels).append(" @ ").append(dm.densityDpi).append("dpi\n");

        long tRam = totalRam(ctx);
        long aRam = availRam(ctx);
        sb.append("RAM: ").append(humanSize(tRam - aRam)).append(" / ").append(humanSize(tRam)).append("\n");

        long totalSt = storageTotal();
        long freeSt = storageFree();
        sb.append("Storage: ").append(humanSize(totalSt - freeSt)).append(" / ").append(humanSize(totalSt)).append("\n");

        int level = batteryLevel(ctx);
        float temp = batteryTemp(ctx);
        sb.append("Battery: ").append(level).append("% · ").append(temp).append("°C\n");

        sb.append("Network: ").append(localIpAddresses()).append("\n");
        sb.append("Wi-Fi: ").append(wifiSsid(ctx)).append("\n");
        sb.append("Apps installed: ").append(appCount(ctx)).append("\n");

        return sb.toString();
    }
}
