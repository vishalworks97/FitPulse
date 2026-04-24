package com.example.fitpulse.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class UserHelper {
    private static final String PREFS_NAME = "FitPulseUser";
    private static final String KEY_SETUP_COMPLETE = "isSetupDone";

    // Set setup as finished
    public static void setProfileComplete(Context context, boolean status) {
        SharedPreferences pref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        pref.edit().putBoolean(KEY_SETUP_COMPLETE, status).apply();
    }

    // Check if user should go to Main or ProfileSetup
    public static boolean isProfileComplete(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return pref.getBoolean(KEY_SETUP_COMPLETE, false);
    }
}