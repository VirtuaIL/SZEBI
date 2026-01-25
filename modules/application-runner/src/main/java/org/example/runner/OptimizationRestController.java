package org.example.runner;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.OptimizationAPI;
import org.example.interfaces.IOptimizationData;
import org.example.interfaces.IUserData;
import org.example.DTO.AdministratorPreferencesDTO;
import org.example.DTO.Uzytkownik;

import java.util.List;

public class OptimizationRestController {
    private final OptimizationAPI optimizationAPI;
    private final IOptimizationData optimizationData;
    private final IUserData userData;
    private final ObjectMapper objectMapper;

    public OptimizationRestController(OptimizationAPI optimizationAPI,
            IOptimizationData optimizationData, IUserData userData) {
        this.optimizationAPI = optimizationAPI;
        this.optimizationData = optimizationData;
        this.userData = userData;
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
            
            // Zapisz preferencje do bazy danych (do użytkownika z rolą administratora)
            List<Uzytkownik> admins = userData.getUsersByRole(1); // 1 = Administrator
            if (!admins.isEmpty()) {
                Uzytkownik admin = admins.get(0);
                
                // Konwertuj AdministratorPreferencesDTO na JSON i zapisz jako preferencje użytkownika
                String prefJson = objectMapper.writeValueAsString(newPref);
                
                // Ustaw preferencje w obiekcie użytkownika jako surowy JSON
                admin.setRawPreferences(prefJson);
                
                // Aktualizuj użytkownika w bazie danych
                Uzytkownik updated = userData.updateUser(admin);
                if (updated == null) {
                    ctx.status(500).json(createErrorResponse("Nie udało się zapisać preferencji do bazy danych"));
                    return;
                }
            } else {
                ctx.status(404).json(createErrorResponse("Nie znaleziono użytkownika z rolą administratora"));
                return;
            }
            
            // Ustaw preferencje w API optymalizacji
            optimizationAPI.setAdminPreferences(newPref);
            
            ctx.status(200).json(newPref);
        } catch (Exception e) {
            ctx.status(400).json(createErrorResponse("Błąd formatu preferencji: " + e.getMessage()));
            e.printStackTrace();
        }
    }
    
    private ObjectNode createErrorResponse(String message) {
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", message);
        return error;
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
