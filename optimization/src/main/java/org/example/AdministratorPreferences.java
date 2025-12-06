package org.example;

public class AdministratorPreferences {
    private double preferredMinTemp;
    private double preferredMaxTemp;
    private String timeOpen;
    private String timeClose;
    private double maxEnergyUsage;
    private int priorityComfort;

    public AdministratorPreferences() {
        this.preferredMinTemp = 18.0;
        this.preferredMaxTemp = 26.0;
        this.timeOpen = "08:00";
        this.timeClose = "20:00";
        this.maxEnergyUsage = 1000.0; // W
        this.priorityComfort = 5;
    }

    public double getPreferredMinTemp() {
        return preferredMinTemp;
    }

    public void setPreferredMinTemp(double preferredMinTemp) {
        this.preferredMinTemp = preferredMinTemp;
    }

    public double getPreferredMaxTemp() {
        return preferredMaxTemp;
    }

    public void setPreferredMaxTemp(double preferredMaxTemp) {
        this.preferredMaxTemp = preferredMaxTemp;
    }

    public String getTimeOpen() {
        return timeOpen;
    }

    public void setTimeOpen(String timeOpen) {
        this.timeOpen = timeOpen;
    }

    public String getTimeClose() {
        return timeClose;
    }

    public void setTimeClose(String timeClose) {
        this.timeClose = timeClose;
    }

    public double getMaxEnergyUsage() {
        return maxEnergyUsage;
    }

    public void setMaxEnergyUsage(double maxEnergyUsage) {
        this.maxEnergyUsage = maxEnergyUsage;
    }

    public int getPriorityComfort() {
        return priorityComfort;
    }

    public void setPriorityComfort(int priorityComfort) {
        this.priorityComfort = priorityComfort;
    }

    public void updatePreference(String key, int value) {
        // Implementacja aktualizacji preferencji
        switch (key) {
            case "priorityComfort":
                this.priorityComfort = value;
                break;
            // Dodaj inne przypadki według potrzeb
        }
    }

    public int getPreference(String key) {
        switch (key) {
            case "priorityComfort":
                return this.priorityComfort;
            default:
                return 0;
        }
    }
}
