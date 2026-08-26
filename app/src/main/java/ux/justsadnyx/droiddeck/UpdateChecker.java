package ux.justsadnyx.droiddeck;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class UpdateChecker {

    private static final String CHANNEL_ID = "droiddeck_updates";
    private static final String WORK_NAME = "droiddeck_update_check";
    private static final int NOTIFICATION_ID = 99;

    public static void schedule(Context ctx) {
        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(
                CheckWorker.class, 1, TimeUnit.HOURS)
                .setInitialDelay(30, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, work);
    }

    private static void ensureChannel(NotificationManager nm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                    "Update checks", NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("Notifies when a new DroidDeck version is available");
            nm.createNotificationChannel(ch);
        }
    }

    static void notifyUpdateAvailable(Context ctx, String version) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        ensureChannel(nm);

        Intent intent = new Intent(ctx, MainActivity.class);
        intent.putExtra("open_updates", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_updates)
                .setContentTitle("DroidDeck update available")
                .setContentText("v" + version + " is ready to install")
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        nm.notify(NOTIFICATION_ID, b.build());
    }

    public static class CheckWorker extends Worker {
        public CheckWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
            super(ctx, params);
        }

        @NonNull
        @Override
        public Result doWork() {
            try {
                String json = fetchUrl("https://api.github.com/repos/justsadnyx-ux/DroidDeck/releases/latest");
                if (json == null) return Result.retry();

                String latestVersion = parseTag(json);
                if (latestVersion == null) return Result.retry();

                String currentVersion = getApplicationContext().getPackageManager()
                        .getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;

                if (isNewer(latestVersion, currentVersion)) {
                    notifyUpdateAvailable(getApplicationContext(), latestVersion);
                }

                return Result.success();
            } catch (Exception e) {
                return Result.retry();
            }
        }

        private String parseTag(String json) {
            int idx = json.indexOf("\"tag_name\"");
            if (idx < 0) return null;
            int q1 = json.indexOf('"', idx + 12);
            if (q1 < 0) return null;
            int q2 = json.indexOf('"', q1 + 1);
            if (q2 < 0) return null;
            String tag = json.substring(q1 + 1, q2);
            return tag.replaceFirst("^v", "");
        }

        private boolean isNewer(String a, String b) {
            try {
                String[] aa = a.split("\\.");
                String[] bb = b.split("\\.");
                int len = Math.max(aa.length, bb.length);
                for (int i = 0; i < len; i++) {
                    int ai = i < aa.length ? Integer.parseInt(aa[i]) : 0;
                    int bi = i < bb.length ? Integer.parseInt(bb[i]) : 0;
                    if (ai > bi) return true;
                    if (ai < bi) return false;
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }

        private String fetchUrl(String urlStr) {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "DroidDeck-Android/1.0");
                BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                r.close();
                conn.disconnect();
                return sb.toString();
            } catch (Exception e) {
                return null;
            }
        }
    }
}
