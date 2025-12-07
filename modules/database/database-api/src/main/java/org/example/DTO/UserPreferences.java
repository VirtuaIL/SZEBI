package org.example.DTO;

import java.util.HashMap;
import java.util.Map;

public class UserPreferences {
    private String userId;
    private int comfortLevel; // Skala 1-5
    private int preferredLighting;
    private Map<String, Integer> preferences;

    public UserPreferences() {
        this.preferences = new HashMap<>();
        this.comfortLevel = 3; // Domyślny komfort (środek skali)
        this.preferredLighting = 70; // Domyślna jasność
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getComfortLevel() {
        return comfortLevel;
    }

    public void setComfortLevel(int comfortLevel) {
        // Simple clamp
        if (comfortLevel < 1)
            comfortLevel = 1;
        if (comfortLevel > 5)
            comfortLevel = 5;
        this.comfortLevel = comfortLevel;
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

    public Map<String, Integer> getPreferences() {
        return preferences;
    }

    public void setPreferences(Map<String, Integer> preferences) {
        this.preferences = preferences;
    }
}
