package org.example.runner;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.example.interfaces.IAlertData;
import org.example.DTO.Alert;
import org.example.DTO.AlertSzczegoly;
import org.example.alerts.AlertService;
import org.example.alerts.dto.ZgloszenieDTO;

import java.util.List;
import java.util.ArrayList;
import java.time.format.DateTimeFormatter;

public class AlertsController {
    private final IAlertData alertData;
    private final AlertService alertService;
    private final ObjectMapper objectMapper;

    public AlertsController(IAlertData alertData) {
        this.alertData = alertData;
        // Inicjalizacja serwisu tutaj pozwala uniknąć zmian w Main.java
        this.alertService = new AlertService(alertData);
        this.objectMapper = new ObjectMapper();
    }

    public void setupRoutes(Javalin app) {
        // --- ENDPOINTY DO ODCZYTU (Bez zmian) ---

        // Pobierz wszystkie alarmy (z opcjonalnymi filtrami)
        app.get("/api/alerts", this::getAllAlerts);

        // Pobierz tylko aktywne alarmy
        app.get("/api/alerts/active", this::getActiveAlerts);

        // Pobierz alarmy według priorytetu
        app.get("/api/alerts/severity/{severity}", this::getAlertsBySeverity);

        // Pobierz szczegóły alarmu po ID
        app.get("/api/alerts/{id}", this::getAlertById);

        // --- ENDPOINTY OPERACYJNE ---

        // Zgłaszanie nowej awarii (Integracja z modułem Analizy)
        app.post("/api/alerts/report", this::zglosAnomalie);

        // Potwierdź alarm (zmień status na POTWIERDZONY)
        app.post("/api/alerts/{id}/acknowledge", this::acknowledgeAlert);

        // Rozwiąż alarm (zmień status na ROZWIAZANY)
        app.post("/api/alerts/{id}/resolve", this::resolveAlert);
    }

    private void zglosAnomalie(Context ctx) {
        try {
            ZgloszenieDTO zgloszenie = ctx.bodyAsClass(ZgloszenieDTO.class);

            alertService.zglosAnomalie(zgloszenie);

            ctx.status(201);
            ObjectNode response = objectMapper.createObjectNode();
            response.put("message", "Zgłoszenie przyjęte");
            ctx.json(response);
        } catch (Exception e) {
            ctx.status(500);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Błąd przetwarzania zgłoszenia: " + e.getMessage());
            ctx.json(error);
            e.printStackTrace();
        }
    }

    private void getAllAlerts(Context ctx) {
        try {
            // Pobierz alarmy dla budynku (jeśli podano) lub wszystkie alarmy
            Integer buildingId = ctx.queryParamAsClass("buildingId", Integer.class).getOrDefault(null);
            List<AlertSzczegoly> alerts;

            if (buildingId != null) {
                // Pobierz alarmy dla konkretnego budynku
                alerts = alertData.getAlertDetailsForBuilding(buildingId);
            } else {
                // Pobierz wszystkie alarmy z wszystkich budynków
                alerts = new ArrayList<>();
                for (int i = 1; i <= 3; i++) {
                    alerts.addAll(alertData.getAlertDetailsForBuilding(i));
                }
            }

            ArrayNode alertsArray = objectMapper.createArrayNode();
            for (AlertSzczegoly alert : alerts) {
                alertsArray.add(convertAlertDetailsToJson(alert));
            }

            ctx.status(200);
            ctx.json(alertsArray);
        } catch (Exception e) {
            ctx.status(500);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Błąd podczas pobierania alarmów: " + e.getMessage());
            ctx.json(error);
            e.printStackTrace();
        }
    }

    private void getActiveAlerts(Context ctx) {
        try {
            // Pobierz wszystkie aktywne alarmy (nie rozwiązane)
            int buildingId = ctx.queryParamAsClass("buildingId", Integer.class).getOrDefault(1);
            List<AlertSzczegoly> allAlerts = alertData.getAlertDetailsForBuilding(buildingId);

            // Filtruj tylko aktywne (status != ROZWIAZANY)
            List<AlertSzczegoly> activeAlerts = allAlerts.stream()
                    .filter(alert -> alert.getStatus() != Alert.AlertStatus.ROZWIAZANY)
                    .toList();

            ArrayNode alertsArray = objectMapper.createArrayNode();
            for (AlertSzczegoly alert : activeAlerts) {
                alertsArray.add(convertAlertDetailsToJson(alert));
            }

            ctx.status(200);
            ctx.json(alertsArray);
        } catch (Exception e) {
            ctx.status(500);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Błąd podczas pobierania aktywnych alarmów: " + e.getMessage());
            ctx.json(error);
            e.printStackTrace();
        }
    }

    private void getAlertsBySeverity(Context ctx) {
        try {
            String severityStr = ctx.pathParam("severity");
            Alert.AlertSeverity severity;

            try {
                severity = Alert.AlertSeverity.valueOf(severityStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                ctx.status(400);
                ObjectNode error = objectMapper.createObjectNode();
                error.put("error", "Nieprawidłowy priorytet. Dostępne: INFO, WARNING, CRITICAL");
                ctx.json(error);
                return;
            }

            List<Alert> alerts = alertData.getActiveAlertsBySeverity(severity);

            ArrayNode alertsArray = objectMapper.createArrayNode();
            for (Alert alert : alerts) {
                alertsArray.add(convertAlertToJson(alert));
            }

            ctx.status(200);
            ctx.json(alertsArray);
        } catch (Exception e) {
            ctx.status(500);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Błąd podczas pobierania alarmów: " + e.getMessage());
            ctx.json(error);
            e.printStackTrace();
        }
    }

    private void getAlertById(Context ctx) {
        try {
            int alertId = ctx.pathParamAsClass("id", Integer.class).get();

            AlertSzczegoly alertDetails = alertData.getAlertDetailsById(alertId);

            if (alertDetails == null) {
                ctx.status(404);
                ObjectNode error = objectMapper.createObjectNode();
                error.put("error", "Alarm o ID " + alertId + " nie został znaleziony");
                ctx.json(error);
                return;
            }

            ctx.status(200);
            ctx.json(convertAlertDetailsToJson(alertDetails));
        } catch (Exception e) {
            ctx.status(500);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Błąd podczas pobierania alarmu: " + e.getMessage());
            ctx.json(error);
            e.printStackTrace();
        }
    }

    private void acknowledgeAlert(Context ctx) {
        try {
            int alertId = ctx.pathParamAsClass("id", Integer.class).get();

            // Delegacja do AlertService
            alertService.potwierdzAlarm(alertId);

            // Pobranie zaktualizowanego stanu do odpowiedzi
            AlertSzczegoly updatedAlert = alertData.getAlertDetailsById(alertId);

            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", true);
            response.put("message", "Alarm został potwierdzony");
            if (updatedAlert != null) {
                response.set("alert", convertAlertDetailsToJson(updatedAlert));
            }

            ctx.status(200);
            ctx.json(response);
        } catch (Exception e) {
            ctx.status(500);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Błąd podczas potwierdzania alarmu: " + e.getMessage());
            ctx.json(error);
            e.printStackTrace();
        }
    }

    private void resolveAlert(Context ctx) {
        try {
            int alertId = ctx.pathParamAsClass("id", Integer.class).get();

            // Delegacja do AlertService
            alertService.rozwiazAlarm(alertId);

            // Pobranie zaktualizowanego stanu do odpowiedzi
            AlertSzczegoly updatedAlert = alertData.getAlertDetailsById(alertId);

            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", true);
            response.put("message", "Alarm został rozwiązany");
            if (updatedAlert != null) {
                response.set("alert", convertAlertDetailsToJson(updatedAlert));
            }

            ctx.status(200);
            ctx.json(response);
        } catch (Exception e) {
            ctx.status(500);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Błąd podczas rozwiązywania alarmu: " + e.getMessage());
            ctx.json(error);
            e.printStackTrace();
        }
    }

    private ObjectNode convertAlertToJson(Alert alert) {
        ObjectNode alertJson = objectMapper.createObjectNode();
        alertJson.put("id", alert.getId());
        alertJson.put("deviceId", alert.getUrzadzenieId());
        alertJson.put("message", alert.getTresc());
        alertJson.put("priority", alert.getPriorytet().name());

        String status = alert.getStatus() != null ? alert.getStatus().name() : "NOWY";
        alertJson.put("status", status);

        if (alert.getCzasAlertu() != null) {
            alertJson.put("timestamp", alert.getCzasAlertu().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        return alertJson;
    }

    private ObjectNode convertAlertDetailsToJson(AlertSzczegoly alert) {
        ObjectNode alertJson = convertAlertToJson(alert);

        if (alert.getNazwaUrzadzenia() != null) {
            alertJson.put("deviceName", alert.getNazwaUrzadzenia());
        }
        if (alert.getNazwaModelu() != null) {
            alertJson.put("deviceModel", alert.getNazwaModelu());
        }
        if (alert.getNazwaPokoju() != null) {
            alertJson.put("roomName", alert.getNazwaPokoju());
        }
        if (alert.getNazwaBudynku() != null) {
            alertJson.put("buildingName", alert.getNazwaBudynku());
        }

        String location = "";
        if (alert.getNazwaPokoju() != null && alert.getNazwaBudynku() != null) {
            location = alert.getNazwaPokoju() + ", " + alert.getNazwaBudynku();
        } else if (alert.getNazwaPokoju() != null) {
            location = alert.getNazwaPokoju();
        } else if (alert.getNazwaBudynku() != null) {
            location = alert.getNazwaBudynku();
        }
        alertJson.put("location", location);

        String deviceDisplayName = "";
        if (alert.getNazwaUrzadzenia() != null) {
            deviceDisplayName = alert.getNazwaUrzadzenia();
            if (alert.getNazwaModelu() != null) {
                deviceDisplayName += " - " + alert.getNazwaModelu();
            }
            if (alert.getNazwaPokoju() != null) {
                deviceDisplayName += " - " + alert.getNazwaPokoju();
            }
        } else {
            deviceDisplayName = "Urządzenie #" + alert.getUrzadzenieId();
        }
        alertJson.put("device", deviceDisplayName);

        return alertJson;
    }
}