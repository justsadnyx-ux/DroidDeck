package ux.justsadnyx.droiddeck;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    private static final String NAME = "droiddeck_prefs";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final String KEY_LAST_UPDATE_CHECK = "last_update_check";

    private static SharedPreferences get(Context ctx) {
        return ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static boolean isOnboardingDone(Context ctx) {
        return get(ctx).getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public static void setOnboardingDone(Context ctx) {
        get(ctx).edit().putBoolean(KEY_ONBOARDING_DONE, true).apply();
    }

    public static long getLastUpdateCheck(Context ctx) {
        return get(ctx).getLong(KEY_LAST_UPDATE_CHECK, 0);
    }

    public static void setLastUpdateCheck(Context ctx, long time) {
        get(ctx).edit().putLong(KEY_LAST_UPDATE_CHECK, time).apply();
    }
}
