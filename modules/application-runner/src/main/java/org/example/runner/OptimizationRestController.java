package org.example.runner;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.OptimizationAPI;
import org.example.interfaces.IOptimizationData;
import org.example.DTO.AdministratorPreferencesDTO;

import java.util.List;

public class OptimizationRestController {
    private final OptimizationAPI optimizationAPI;
    private final IOptimizationData optimizationData;
    private final ObjectMapper objectMapper;

    public OptimizationRestController(OptimizationAPI optimizationAPI,
            IOptimizationData optimizationData) {
        this.optimizationAPI = optimizationAPI;
        this.optimizationData = optimizationData;
        this.objectMapper = new ObjectMapper();
    }

    public void setupRoutes(Javalin app) {
        // Status i konfiguracja
        app.get("/api/optimization/config", this::getConfig);
        app.post("/api/optimization/config", this::updateConfig);

        // Kontrola cyklu
        app.post("/api/optimization/start", this::startCycle);
        app.post("/api/optimization/stop", this::stopCycle);
        app.post("/api/optimization/run", this::triggerManualRun);

        // Override
        app.get("/api/optimization/override", this::getOverride);
        app.post("/api/optimization/override", this::setOverride);
    }

    private void getConfig(Context ctx) {
        ctx.json(optimizationAPI.getAdminPreferences());
    }

    private void updateConfig(Context ctx) {
        try {
            AdministratorPreferencesDTO newPref = ctx.bodyAsClass(AdministratorPreferencesDTO.class);
            optimizationAPI.setAdminPreferences(newPref);
            ctx.status(200).json(newPref);
        } catch (Exception e) {
            ctx.status(400).result("Błąd formatu preferencji: " + e.getMessage());
        }
    }

    private void startCycle(Context ctx) {
        int buildingId = ctx.queryParamAsClass("buildingId", Integer.class).getOrDefault(1);
        int interval = ctx.queryParamAsClass("interval", Integer.class).getOrDefault(60);
        optimizationAPI.startAutoCycle(buildingId, interval);
        ctx.status(200).result("Automatyczny cykl uruchomiony.");
    }

    private void stopCycle(Context ctx) {
        optimizationAPI.stopAutoCycle();
        ctx.status(200).result("Automatyczny cykl zatrzymany.");
    }

    private void triggerManualRun(Context ctx) {
        int buildingId = ctx.queryParamAsClass("buildingId", Integer.class).getOrDefault(1);
        optimizationAPI.runManualOptimization(buildingId);
        ctx.status(200).result("Optymalizacja wykonana ręcznie.");
    }

    private void getOverride(Context ctx) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("override", optimizationAPI.isOverrideAutomatization());
        ctx.json(response);
    }

    private void setOverride(Context ctx) {
        boolean override = ctx.queryParamAsClass("enabled", Boolean.class).getOrDefault(true);
        optimizationAPI.setOverrideAutomatization(override);
        ctx.status(200).result("Tryb override ustawiony na: " + override);
    }

}
