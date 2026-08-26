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

import java.util.concurrent.TimeUnit;

public class UpdateChecker {

    private static final String CHANNEL_ID = "droiddeck_updates";
    private static final String WORK_NAME = "droiddeck_update_check";
    private static final int NOTIFICATION_ID = 99;

    public static void schedule(Context ctx) {
        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(
                CheckWorker.class, 6, TimeUnit.HOURS)
                .setInitialDelay(1, TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, work);
    }

    public static void checkNow(Context ctx) {
        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(
                CheckWorker.class, 6, TimeUnit.HOURS)
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
                String json = Util.fetchUrl("https://api.github.com/repos/justsadnyx-ux/DroidDeck/releases/latest");
                if (json == null) return Result.retry();

                String tagName = extractTag(json);
                if (tagName == null) return Result.retry();

                String latestVersion = tagName.replaceFirst("^v", "");
                String currentVersion = getApplicationContext().getPackageManager()
                        .getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;

                if (isNewer(latestVersion, currentVersion)) {
                    notifyUpdateAvailable(getApplicationContext(), latestVersion);
                }

                Prefs.setLastUpdateCheck(getApplicationContext(), System.currentTimeMillis());
                return Result.success();
            } catch (Exception e) {
                return Result.retry();
            }
        }

        private String extractTag(String json) {
            int idx = json.indexOf("\"tag_name\"");
            if (idx < 0) return null;
            int colon = json.indexOf(':', idx + 11);
            int start = json.indexOf('"', colon + 1);
            int end = json.indexOf('"', start + 1);
            if (start < 0 || end < 0) return null;
            return json.substring(start + 1, end);
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
    }
}
