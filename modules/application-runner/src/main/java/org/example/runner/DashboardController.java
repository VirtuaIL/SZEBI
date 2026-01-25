package org.example.runner;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.example.interfaces.IAnalyticsData;
import org.example.DTO.Odczyt;
import org.example.DTO.Prognoza;
import org.example.DTO.AlertSzczegoly;
import org.example.DTO.Alert;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class DashboardController {
    private final IAnalyticsData analyticsData;
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public DashboardController(IAnalyticsData analyticsData) {
        this.analyticsData = analyticsData;
        this.objectMapper = new ObjectMapper();
    }

    public void setupRoutes(Javalin app) {
        // Agregowany endpoint dla wszystkich danych dashboardu
        app.get("/api/dashboard/summary", this::getDashboardSummary);
        
        // Endpoint dla zużycia energii (rzeczywiste + prognozowane)
        app.get("/api/dashboard/energy", this::getEnergyData);
        
        // Endpoint dla statusu OZE
        app.get("/api/dashboard/oze-status", this::getOZEStatus);
        
        // Endpoint dla anomalii
        app.get("/api/dashboard/anomalies", this::getAnomalies);
    }

    /**
     * GET /api/dashboard/summary?buildingId=1&range=day|week|month
     * Zwraca agregowane dane dla dashboardu
     */
    private void getDashboardSummary(Context ctx) {
        try {
            int buildingId = ctx.queryParamAsClass("buildingId", Integer.class).getOrDefault(1);
            String range = ctx.queryParam("range");
            if (range == null || range.isEmpty()) {
                range = "day";
            }

            LocalDateTime[] dateRange = calculateDateRange(range);
            LocalDateTime from = dateRange[0];
            LocalDateTime to = dateRange[1];

            // Pobierz dane energii
            ObjectNode energyData = getEnergyDataInternal(buildingId, from, to, range);
            
            // Pobierz status OZE
            ObjectNode ozeStatus = getOZEStatusInternal(buildingId);
            
            // Pobierz anomalie
            ArrayNode anomalies = getAnomaliesInternal(buildingId, from, to);

            // Oblicz statystyki
            double totalConsumption = energyData.has("totalConsumption") 
                ? energyData.get("totalConsumption").asDouble() : 0.0;
            double avgConsumption = energyData.has("averageConsumption")
                ? energyData.get("averageConsumption").asDouble() : 0.0;

            ObjectNode response = objectMapper.createObjectNode();
            response.set("energy", energyData);
            response.set("ozeStatus", ozeStatus);
            response.set("anomalies", anomalies);
            response.put("totalConsumption", totalConsumption);
            response.put("averageConsumption", avgConsumption);
            response.put("anomaliesCount", anomalies.size());
            response.put("range", range);
            response.put("buildingId", buildingId);

            ctx.status(200);
            ctx.json(response);
        } catch (Exception e) {
            ctx.status(500);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Błąd podczas pobierania danych dashboardu: " + e.getMessage());
            ctx.json(error);
            e.printStackTrace();
        }
    }

    /**
     * GET /api/dashboard/energy?buildingId=1&range=day|week|month
     * Zwraca dane zużycia energii (rzeczywiste i prognozowane)
     */
    private void getEnergyData(Context ctx) {
        try {
            int buildingId = ctx.queryParamAsClass("buildingId", Integer.class).getOrDefault(1);
            String range = ctx.queryParam("range");
            if (range == null || range.isEmpty()) {
                range = "day";
            }

            LocalDateTime[] dateRange = calculateDateRange(range);
            LocalDateTime from = dateRange[0];
            LocalDateTime to = dateRange[1];

            ObjectNode response = getEnergyDataInternal(buildingId, from, to, range);
            ctx.status(200);
            ctx.json(response);
        } catch (Exception e) {
            ctx.status(500);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Błąd podczas pobierania danych energii: " + e.getMessage());
            ctx.json(error);
            e.printStackTrace();
        }
    }

    private ObjectNode getEnergyDataInternal(int buildingId, LocalDateTime from, LocalDateTime to, String range) {
        // Pobierz rzeczywiste odczyty
        List<Odczyt> readings = analyticsData.getReadingsForBuilding(buildingId, from, to);
        
        // Pobierz prognozy
        List<Prognoza> forecasts = analyticsData.getForecastsForBuilding(buildingId, from, to);

        // Agreguj dane rzeczywiste według przedziału czasowego
        ArrayNode energyDataArray = objectMapper.createArrayNode();
        Map<String, List<Double>> realDataMap = new HashMap<>();
        Map<String, List<Double>> forecastDataMap = new HashMap<>();

        // Grupuj odczyty rzeczywiste
        for (Odczyt reading : readings) {
            try {
                String timeKey = formatTimeKey(reading.getCzasOdczytu().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime(), range);
                double value = extractEnergyValue(reading.getPomiary());
                realDataMap.computeIfAbsent(timeKey, k -> new ArrayList<>()).add(value);
            } catch (Exception e) {
                // Ignoruj błędy parsowania
            }
        }

        // Grupuj prognozy
        for (Prognoza forecast : forecasts) {
            try {
                String timeKey = formatTimeKey(forecast.getCzasPrognozy(), range);
                double value = forecast.getPrognozowanaWartosc();
                forecastDataMap.computeIfAbsent(timeKey, k -> new ArrayList<>()).add(value);
            } catch (Exception e) {
                // Ignoruj błędy
            }
        }

        // Utwórz zunifikowane dane
        java.util.Set<String> allTimeKeys = new java.util.HashSet<>();
        allTimeKeys.addAll(realDataMap.keySet());
        allTimeKeys.addAll(forecastDataMap.keySet());

        double totalReal = 0.0;
        double totalForecast = 0.0;
        int count = 0;

        for (String timeKey : allTimeKeys) {
            double realAvg = realDataMap.containsKey(timeKey) 
                ? realDataMap.get(timeKey).stream().mapToDouble(Double::doubleValue).average().orElse(0.0)
                : 0.0;
            double forecastAvg = forecastDataMap.containsKey(timeKey)
                ? forecastDataMap.get(timeKey).stream().mapToDouble(Double::doubleValue).average().orElse(0.0)
                : 0.0;

            // Konwersja z W na kWh (zakładając że to wartość godzinowa)
            double realKwh = realAvg / 1000.0;
            double forecastKwh = forecastAvg / 1000.0;

            ObjectNode dataPoint = objectMapper.createObjectNode();
            dataPoint.put(range.equals("day") ? "hour" : "day", timeKey);
            dataPoint.put("rzeczywiste", Math.round(realKwh * 100.0) / 100.0);
            dataPoint.put("prognozowane", Math.round(forecastKwh * 100.0) / 100.0);
            energyDataArray.add(dataPoint);

            totalReal += realKwh;
            totalForecast += forecastKwh;
            count++;
        }

        ObjectNode response = objectMapper.createObjectNode();
        response.set("data", energyDataArray);
        response.put("totalConsumption", Math.round(totalReal * 100.0) / 100.0);
        response.put("averageConsumption", count > 0 ? Math.round((totalReal / count) * 100.0) / 100.0 : 0.0);
        response.put("totalForecast", Math.round(totalForecast * 100.0) / 100.0);
        response.put("range", range);
        response.put("buildingId", buildingId);

        return response;
    }

    /**
     * GET /api/dashboard/oze-status?buildingId=1
     * Zwraca status OZE (produkcja i pobór z sieci)
     */
    private void getOZEStatus(Context ctx) {
        try {
            int buildingId = ctx.queryParamAsClass("buildingId", Integer.class).getOrDefault(1);
            ObjectNode response = getOZEStatusInternal(buildingId);
            ctx.status(200);
            ctx.json(response);
        } catch (Exception e) {
            ctx.status(500);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Błąd podczas pobierania statusu OZE: " + e.getMessage());
            ctx.json(error);
            e.printStackTrace();
        }
    }

    private ObjectNode getOZEStatusInternal(int buildingId) {
        // Oblicz produkcję OZE (symulacja - można później zintegrować z rzeczywistymi danymi)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusHours(1);
        
        // Pobierz całkowite zużycie z ostatniej godziny
        double totalConsumption = analyticsData.getTotalEnergyConsumptionForBuilding(buildingId, from, now);
        
        // Symulacja produkcji OZE (można później zastąpić rzeczywistymi danymi)
        // Zakładamy że OZE pokrywa część zużycia w zależności od pory dnia
        int hour = now.getHour();
        double solarProduction = 0.0;
        if (hour >= 6 && hour <= 18) {
            // W ciągu dnia symulujemy produkcję słoneczną
            solarProduction = Math.max(0, 300 + (Math.sin((hour - 6) * Math.PI / 12) * 200));
        }
        
        // Pobór z sieci = całkowite zużycie - produkcja OZE
        double gridConsumption = Math.max(0, totalConsumption * 1000 - solarProduction); // Konwersja kWh na W
        
        ObjectNode response = objectMapper.createObjectNode();
        response.put("production", Math.round(solarProduction));
        response.put("grid", Math.round(gridConsumption));
        response.put("buildingId", buildingId);
        
        return response;
    }

    /**
     * GET /api/dashboard/anomalies?buildingId=1&range=day|week|month
     * Zwraca wykryte anomalie
     */
    private void getAnomalies(Context ctx) {
        try {
            int buildingId = ctx.queryParamAsClass("buildingId", Integer.class).getOrDefault(1);
            String range = ctx.queryParam("range");
            if (range == null || range.isEmpty()) {
                range = "day";
            }

            LocalDateTime[] dateRange = calculateDateRange(range);
            LocalDateTime from = dateRange[0];
            LocalDateTime to = dateRange[1];

            ArrayNode response = getAnomaliesInternal(buildingId, from, to);
            ctx.status(200);
            ctx.json(response);
        } catch (Exception e) {
            ctx.status(500);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Błąd podczas pobierania anomalii: " + e.getMessage());
            ctx.json(error);
            e.printStackTrace();
        }
    }

    private ArrayNode getAnomaliesInternal(int buildingId, LocalDateTime from, LocalDateTime to) {
        List<AlertSzczegoly> alerts = analyticsData.getAlertDetailsForBuilding(buildingId, from, to);
        ArrayNode anomaliesArray = objectMapper.createArrayNode();

        for (AlertSzczegoly alert : alerts) {
            // Filtruj tylko alarmy związane z anomaliami (WARNING i CRITICAL)
            if (alert.getPriorytet() != null && 
                (alert.getPriorytet() == Alert.AlertSeverity.WARNING || 
                 alert.getPriorytet() == Alert.AlertSeverity.CRITICAL)) {
                ObjectNode anomaly = objectMapper.createObjectNode();
                anomaly.put("time", alert.getCzasAlertu() != null 
                    ? alert.getCzasAlertu().format(DateTimeFormatter.ofPattern("HH:mm"))
                    : "N/A");
                // Użyj wartości z treści alertu lub domyślnej wartości
                anomaly.put("value", 1000.0); // Domyślna wartość, można później wyciągnąć z treści
                anomaly.put("type", alert.getPriorytet() == Alert.AlertSeverity.CRITICAL ? "high" : "medium");
                anomaly.put("deviceName", alert.getNazwaUrzadzenia() != null ? alert.getNazwaUrzadzenia() : "N/A");
                anomaliesArray.add(anomaly);
            }
        }

        return anomaliesArray;
    }

    private LocalDateTime[] calculateDateRange(String range) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from;

        switch (range.toLowerCase()) {
            case "week":
                from = now.minusDays(7);
                break;
            case "month":
                from = now.minusDays(30);
                break;
            case "day":
            default:
                from = now.minusDays(1);
                break;
        }

        return new LocalDateTime[] { from, now };
    }

    private String formatTimeKey(LocalDateTime dateTime, String range) {
        if ("day".equals(range)) {
            return String.format("%02d:00", dateTime.getHour());
        } else {
            return "Dzień " + dateTime.getDayOfMonth();
        }
    }

    private double extractEnergyValue(String pomiaryJson) {
        try {
            JsonNode root = objectMapper.readTree(pomiaryJson);
            if (root.has("zuzycie_kwh")) {
                return root.get("zuzycie_kwh").asDouble() * 1000; // Konwersja kWh na W
            } else if (root.has("wartosc")) {
                return root.get("wartosc").asDouble();
            } else if (root.has("value")) {
                return root.get("value").asDouble();
            } else if (root.has("power")) {
                return root.get("power").asDouble();
            }
        } catch (Exception e) {
            // Ignoruj błędy parsowania
        }
        return 0.0;
    }
}
