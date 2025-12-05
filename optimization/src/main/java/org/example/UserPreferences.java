package org.example;

import java.util.HashMap;
import java.util.Map;

public class UserPreferences {
    private String userId;
    private int preferredTemperature;
    private int preferredLighting;
    private Map<String, Integer> preferences;

    public UserPreferences() {
        this.preferences = new HashMap<>();
        this.preferredTemperature = 22; // Domyślna temperatura
        this.preferredLighting = 70; // Domyślna jasność
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getPreferredTemperature() {
        return preferredTemperature;
    }

    public void setPreferredTemperature(int preferredTemperature) {
        this.preferredTemperature = preferredTemperature;
    }

    public int getPreferredLighting() {
        return preferredLighting;
    }

    public void setPreferredLighting(int preferredLighting) {
        this.preferredLighting = preferredLighting;
    }

    public void updatePreference(String key, int value) {
        preferences.put(key, value);
    }

    public int getPreference(String key) {
        return preferences.getOrDefault(key, 0);
    }
}