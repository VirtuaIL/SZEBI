package org.example;

import org.example.DTO.AdministratorPreferencesDTO;
import org.example.interfaces.*;
import org.example.alerts.AlertService;

public class OptimizationAPI {

    private final OptimizationController controller;

    public OptimizationAPI() {
        this.controller = new OptimizationController();
    }

    // --- Configuration & Dependencies ---

    public void setAcquisitionAPI(AcquisitionAPI api) {
        controller.setAcquisitionAPI(api);
    }

    public void setAlertService(AlertService service) {
        controller.setAlertService(service);
    }

    public void setAcquisitionService(IAcquisitionData service) {
        controller.setAcquisitionService(service);
    }

    public void setForecastService(IForecastingData service) {
        controller.setForecastService(service);
    }

    public void setControlService(IControlData service) {
        controller.setControlService(service);
    }

    public void setAnalyticsService(IAnalyticsData service) {
        controller.setAnalyticsService(service);
    }

    public void setUserService(IUserData service) {
        controller.setUserService(service);
    }

    public void setOptimizationData(IOptimizationData data) {
        controller.setOptimizationData(data);
    }

    public void setForecastServiceAPI(ForecastServiceAPI api) {
        controller.setForecastServiceAPI(api);
    }

    // --- Control Methods ---

    public void startAutoCycle(int buildingId, int intervalSeconds) {
        controller.startAutoCycle(buildingId, intervalSeconds);
    }

    public void stopAutoCycle() {
        controller.stopAutoCycle();
    }

    /**
     * Trigger manual optimization run for a building.
     */
    public void runManualOptimization(int buildingId) {
        controller.optimizeEnergyConsumption(buildingId);
        controller.checkForAnomalies();
    }

    // --- Preferences & State ---

    public AdministratorPreferencesDTO getAdminPreferences() {
        return controller.getAdminPref();
    }

    public void setAdminPreferences(AdministratorPreferencesDTO preferences) {
        controller.setAdminPreferences(preferences);
    }

    public boolean isOverrideAutomatization() {
        return controller.isOverrideAutomatization();
    }

    public void setOverrideAutomatization(boolean override) {
        controller.setOverrideAutomatization(override);
    }
}
