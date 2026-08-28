package ux.justsadnyx.droiddeck;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    private static final String NAME = "droiddeck_prefs";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final String KEY_LAST_UPDATE_CHECK = "last_update_check";
    private static final String KEY_TERMS_ACCEPTED = "terms_accepted";
    private static final String KEY_TERMS_VERSION = "terms_version";
    private static final String KEY_TERMS_INSTALLED = "terms_installed";
    private static final String KEY_TERMS_INSTALL_REQUESTED = "terms_install_requested";

    private static SharedPreferences get(Context ctx) {
        return ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static boolean isOnboardingDone(Context ctx) {
        return get(ctx).getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public static void setOnboardingDone(Context ctx) {
        get(ctx).edit().putBoolean(KEY_ONBOARDING_DONE, true).apply();
    }

    public static boolean areTermsAccepted(Context ctx) {
        return get(ctx).getBoolean(KEY_TERMS_ACCEPTED, false);
    }

    public static void setTermsAccepted(Context ctx) {
        get(ctx).edit().putBoolean(KEY_TERMS_ACCEPTED, true).apply();
    }

    public static void setTermsVersion(Context ctx, int version) {
        get(ctx).edit().putInt(KEY_TERMS_VERSION, version).apply();
    }

    public static int getTermsInstalledVersion(Context ctx) {
        return get(ctx).getInt(KEY_TERMS_VERSION, 0);
    }

    public static boolean isTermsInstalled(Context ctx) {
        return get(ctx).getBoolean(KEY_TERMS_INSTALLED, false);
    }

    public static void setTermsInstalled(Context ctx, boolean installed) {
        get(ctx).edit().putBoolean(KEY_TERMS_INSTALLED, installed).apply();
    }

    public static boolean wasTermsInstallRequested(Context ctx) {
        return get(ctx).getBoolean(KEY_TERMS_INSTALL_REQUESTED, false);
    }

    public static void setTermsInstallRequested(Context ctx, boolean requested) {
        get(ctx).edit().putBoolean(KEY_TERMS_INSTALL_REQUESTED, requested).apply();
    }

    public static long getLastUpdateCheck(Context ctx) {
        return get(ctx).getLong(KEY_LAST_UPDATE_CHECK, 0);
    }

    public static void setLastUpdateCheck(Context ctx, long time) {
        get(ctx).edit().putLong(KEY_LAST_UPDATE_CHECK, time).apply();
    }
}
