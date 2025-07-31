package com.example.wisewallet;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

public class PreferencesManager {

    private static final String PREF_NAME = "WiseWalletPrefs";
    private static final String DARK_MODE_KEY = "dark_mode";
    private static final String NOTIFICATIONS_KEY = "notifications";
    private static final String THEME_COLOR_KEY = "theme_color";
    private static final String CURRENCY_KEY = "currency";
    private static final String OFFLINE_MPESA_KEY = "offline_mpesa_messages";
    private static final String ONBOARDING_COMPLETED_KEY = "onboarding_completed";
    private static final String KEY_IS_PREMIUM = "is_premium";


    private final SharedPreferences prefs;

    public PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ==================== Theme & Settings ====================

    public boolean isDarkModeEnabled() {
        return prefs.getBoolean(DARK_MODE_KEY, false);
    }

    public void setDarkModeEnabled(boolean enabled) {
        prefs.edit().putBoolean(DARK_MODE_KEY, enabled).apply();
    }

    public boolean areNotificationsEnabled() {
        return prefs.getBoolean(NOTIFICATIONS_KEY, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(NOTIFICATIONS_KEY, enabled).apply();
    }

    public String getThemeColor() {
        return prefs.getString(THEME_COLOR_KEY, "Default");
    }

    public void setThemeColor(String color) {
        prefs.edit().putString(THEME_COLOR_KEY, color).apply();
    }

    public String getCurrency() {
        return prefs.getString(CURRENCY_KEY, "KES");
    }

    public void setCurrency(String currencyCode) {
        prefs.edit().putString(CURRENCY_KEY, currencyCode).apply();
    }

    // ==================== Offline M-Pesa Messages ====================

    public void saveOfflineMpesaMessage(String message) {
        try {
            JSONArray jsonArray = getOfflineMpesaMessages();
            jsonArray.put(message);
            prefs.edit().putString(OFFLINE_MPESA_KEY, jsonArray.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONArray getOfflineMpesaMessages() throws JSONException {
        String json = prefs.getString(OFFLINE_MPESA_KEY, "[]");
        return new JSONArray(json);
    }

    public void clearOfflineMpesaMessages() {
        prefs.edit().remove(OFFLINE_MPESA_KEY).apply();
    }

    // Check if user has completed onboarding
    public boolean isOnboardingCompleted() {
        return prefs.getBoolean(ONBOARDING_COMPLETED_KEY, false);
    }

    // Mark onboarding as completed
    public void setOnboardingCompleted(boolean completed) {
        prefs.edit().putBoolean(ONBOARDING_COMPLETED_KEY, completed).apply();
    }

    // ---- PREMIUM ----
    public void setPremium(boolean isPremium) {
        prefs.edit().putBoolean(KEY_IS_PREMIUM, isPremium).apply();
    }

    public boolean isPremium() {
        return prefs.getBoolean(KEY_IS_PREMIUM, false);
    }
}
