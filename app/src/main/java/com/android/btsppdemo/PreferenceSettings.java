package com.android.btsppdemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferenceSettings {
    private static final String SECURITY = "SECURITY";

    public static boolean getSecurity(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(SECURITY, true);
    }

    public static void setSecurity(Context context, boolean secure) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean _secure = getSecurity(context);
        if (secure ^ _secure) {
            prefs.edit().putBoolean(SECURITY, secure).apply();
        }
    }
}
