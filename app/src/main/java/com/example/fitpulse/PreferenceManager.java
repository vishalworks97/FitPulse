package com.example.fitpulse;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PreferenceManager {
    private static final String PREF_NAME = "fitpulse-prefs";
    private static final String KEY_IS_PROFILE_COMPLETE = "is_profile_complete";
    private static final String KEY_STEPS_OFFSET = "steps_offset";
    private static final String KEY_LAST_RESET_DATE = "last_reset_date";
    private static final String KEY_LAST_STEP_RESET_DATE = "last_step_reset_date";
    
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_BMR = "user_bmr";
    private static final String KEY_USER_WEIGHT = "user_weight";
    private static final String KEY_USER_HEIGHT = "user_height";
    private static final String KEY_USER_AGE = "user_age";
    private static final String KEY_USER_GENDER = "user_gender";
    private static final String KEY_USER_STATUS = "user_status";
    private static final String KEY_IS_VEGETARIAN = "is_vegetarian";

    // Daily Stats Cache
    private static final String KEY_DAILY_INTAKE = "daily_intake";
    private static final String KEY_DAILY_BURNT_WORKOUT = "daily_burnt_workout";
    private static final String KEY_DAILY_STEPS = "daily_steps";

    // Notification Settings
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_BREAKFAST_TIME = "breakfast_time";
    private static final String KEY_LUNCH_TIME = "lunch_time";
    private static final String KEY_DINNER_TIME = "dinner_time";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public PreferenceManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void clearAll() {
        editor.clear().apply();
    }

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    public void checkAndResetDailyStats() {
        String today = getTodayDate();
        String lastReset = pref.getString(KEY_LAST_RESET_DATE, "");
        if (!today.equals(lastReset)) {
            editor.putString(KEY_LAST_RESET_DATE, today);
            editor.putFloat(KEY_DAILY_INTAKE, 0f);
            editor.putFloat(KEY_DAILY_BURNT_WORKOUT, 0f);
            editor.putInt(KEY_DAILY_STEPS, 0);
            editor.apply();
        }
    }

    public void updateStepResetDate() {
        editor.putString(KEY_LAST_STEP_RESET_DATE, getTodayDate());
        editor.apply();
    }

    public void saveStepsOffset(int offset) {
        editor.putInt(KEY_STEPS_OFFSET, offset);
        editor.apply();
    }

    public int getStepsOffset() { return pref.getInt(KEY_STEPS_OFFSET, 0); }

    public void cacheUserData(String name, double bmr) {
        editor.putString(KEY_USER_NAME, name);
        editor.putFloat(KEY_USER_BMR, (float) bmr);
        editor.apply();
    }

    public void saveFullProfile(String name, String gender, float weight, float height, int age, String status) {
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_GENDER, gender);
        editor.putFloat(KEY_USER_WEIGHT, weight);
        editor.putFloat(KEY_USER_HEIGHT, height);
        editor.putInt(KEY_USER_AGE, age);
        editor.putString(KEY_USER_STATUS, status);
        editor.apply();
    }

    public String getUserName() { return pref.getString(KEY_USER_NAME, "User"); }
    public String getUserGender() { return pref.getString(KEY_USER_GENDER, "Male"); }
    public float getUserWeight() { return pref.getFloat(KEY_USER_WEIGHT, 70f); }
    public float getUserHeight() { return pref.getFloat(KEY_USER_HEIGHT, 170f); }
    public int getUserAge() { return pref.getInt(KEY_USER_AGE, 25); }
    public String getUserStatus() { return pref.getString(KEY_USER_STATUS, "Active"); }
    public float getUserBmr() { return pref.getFloat(KEY_USER_BMR, 2000f); }

    public boolean isVegetarian() { return pref.getBoolean(KEY_IS_VEGETARIAN, false); }
    public void setVegetarian(boolean vegetarian) {
        editor.putBoolean(KEY_IS_VEGETARIAN, vegetarian);
        editor.apply();
    }

    public void setIntake(float value) {
        editor.putFloat(KEY_DAILY_INTAKE, value);
        editor.apply();
    }
    public float getIntake() { return pref.getFloat(KEY_DAILY_INTAKE, 0f); }

    public void setWorkoutBurnt(float value) {
        editor.putFloat(KEY_DAILY_BURNT_WORKOUT, value);
        editor.apply();
    }
    public float getWorkoutBurnt() { return pref.getFloat(KEY_DAILY_BURNT_WORKOUT, 0f); }

    public void setSteps(int steps) {
        editor.putInt(KEY_DAILY_STEPS, steps);
        editor.apply();
    }
    public int getSteps() { return pref.getInt(KEY_DAILY_STEPS, 0); }
    
    public boolean shouldResetSteps() {
        return !getTodayDate().equals(pref.getString(KEY_LAST_STEP_RESET_DATE, ""));
    }

    public boolean isProfileComplete() {
        return pref.getBoolean(KEY_IS_PROFILE_COMPLETE, false);
    }

    public void setProfileComplete(boolean complete) {
        editor.putBoolean(KEY_IS_PROFILE_COMPLETE, complete);
        editor.apply();
    }

    // Notifications
    public boolean isNotificationsEnabled() {
        return pref.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        editor.putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled);
        editor.apply();
    }

    public String getBreakfastTime() { return pref.getString(KEY_BREAKFAST_TIME, "09:00"); }
    public void setBreakfastTime(String time) { editor.putString(KEY_BREAKFAST_TIME, time); editor.apply(); }

    public String getLunchTime() { return pref.getString(KEY_LUNCH_TIME, "14:00"); }
    public void setLunchTime(String time) { editor.putString(KEY_LUNCH_TIME, time); editor.apply(); }

    public String getDinnerTime() { return pref.getString(KEY_DINNER_TIME, "21:00"); }
    public void setDinnerTime(String time) { editor.putString(KEY_DINNER_TIME, time); editor.apply(); }
}
