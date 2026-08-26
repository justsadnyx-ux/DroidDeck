package ux.justsadnyx.droiddeck;

import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.os.StatFs;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

public class DashboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        TextView device = v.findViewById(R.id.d_device);
        TextView androidVer = v.findViewById(R.id.d_android);
        TextView cpu = v.findViewById(R.id.d_cpu);
        TextView screen = v.findViewById(R.id.d_screen);
        TextView ram = v.findViewById(R.id.d_ram);
        ProgressBar ramBar = v.findViewById(R.id.d_ram_bar);
        TextView storage = v.findViewById(R.id.d_storage);
        ProgressBar storageBar = v.findViewById(R.id.d_storage_bar);
        TextView battery = v.findViewById(R.id.d_battery);
        TextView uptime = v.findViewById(R.id.d_uptime);
        TextView ipLocal = v.findViewById(R.id.d_ip_local);
        TextView ipPublic = v.findViewById(R.id.d_ip_public);
        TextView wifiSsid = v.findViewById(R.id.d_wifi_ssid);
        TextView batteryHealth = v.findViewById(R.id.d_battery_health);
        TextView appCount = v.findViewById(R.id.d_app_count);
        MaterialButton refresh = v.findViewById(R.id.d_refresh);
        MaterialButton copyReport = v.findViewById(R.id.d_copy_report);

        device.setText(Build.MANUFACTURER + " " + Build.MODEL);
        String patch = Build.VERSION.SECURITY_PATCH != null ? " · security " + Build.VERSION.SECURITY_PATCH : "";
        androidVer.setText("Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")" + patch);
        cpu.setText(Util.cpuInfo());

        int densityDpi = getResources().getDisplayMetrics().densityDpi;
        int w = getResources().getDisplayMetrics().widthPixels;
        int h = getResources().getDisplayMetrics().heightPixels;
        screen.setText(w + " × " + h + " px · " + densityDpi + " dpi");

        ipLocal.setText(Util.localIpAddresses());
        Util.fetchPublicIp(ipPublic::setText);
        wifiSsid.setText(Util.wifiSsid(requireContext()));
        appCount.setText(String.valueOf(Util.appCount(requireContext())));

        refresh.setOnClickListener(btn -> {
            ipLocal.setText(Util.localIpAddresses());
            Util.fetchPublicIp(ipPublic::setText);
            wifiSsid.setText(Util.wifiSsid(requireContext()));
            updateLive(ram, ramBar, storage, storageBar, battery, uptime, batteryHealth);
        });

        copyReport.setOnClickListener(btn -> {
            ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("report", Util.deviceReport(requireContext())));
                android.widget.Toast.makeText(requireContext(), "Report copied", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        updateLive(ram, ramBar, storage, storageBar, battery, uptime, batteryHealth);
    }

    private void updateLive(TextView ram, ProgressBar ramBar, TextView storage, ProgressBar storageBar,
                            TextView battery, TextView uptime, TextView batteryHealth) {
        Context ctx = requireContext();

        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        long totalRam = mi.totalMem;
        long availRam = mi.availMem;
        long usedRam = totalRam - availRam;
        ram.setText(Util.humanSize(usedRam) + " used of " + Util.humanSize(totalRam));
        ramBar.setProgress((int) (usedRam * 100 / Math.max(totalRam, 1)));

        long totalSt = Util.storageTotal();
        long freeSt = Util.storageFree();
        long usedSt = totalSt - freeSt;
        storage.setText(Util.humanSize(usedSt) + " used of " + Util.humanSize(totalSt)
                + " (" + Util.humanSize(freeSt) + " free)");
        storageBar.setProgress((int) (usedSt * 100 / Math.max(totalSt, 1)));

        BatteryManager bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
        int level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        float temp = Util.batteryTemp(ctx);
        battery.setText(level + "% charged · " + temp + " °C");

        android.content.Intent intent = ctx.registerReceiver(null, new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
        String health = "Unknown";
        if (intent != null) {
            int hp = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
            health = switch (hp) {
                case BatteryManager.BATTERY_HEALTH_GOOD -> "Good";
                case BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating";
                case BatteryManager.BATTERY_HEALTH_DEAD -> "Dead";
                case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage";
                case BatteryManager.BATTERY_HEALTH_COLD -> "Cold";
                default -> "Unknown";
            };
            boolean charging = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING;
            int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            String source = "";
            if (charging) {
                source = plugged == BatteryManager.BATTERY_PLUGGED_AC ? " (AC)" :
                         plugged == BatteryManager.BATTERY_PLUGGED_USB ? " (USB)" :
                         plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS ? " (Wireless)" : "";
            }
            health += source;
            int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
            if (voltage > 0) health += " · " + voltage + "mV";
        }
        batteryHealth.setText(health);

        uptime.setText("Up for " + Util.humanDuration(SystemClock.elapsedRealtime()));
    }
}
