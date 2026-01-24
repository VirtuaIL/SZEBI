package org.example;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.example.DTO.Prognoza;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Controller REST API dla modułu prognozowania.
 * Wystawia endpointy do komunikacji z GUI i innymi modułami.
 */
public class ForecastController {
    
    private final ForecastServiceAPI forecastService;
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public ForecastController(ForecastServiceAPI forecastService) {
        this.forecastService = forecastService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Rejestruje endpointy REST w aplikacji Javalin.
     */
    public void setupRoutes(Javalin app) {
        // Generowanie prognozy dla urządzenia
        app.post("/api/forecasts/generate/{deviceId}", this::generateForecast);
        
        // Pobieranie prognoz dla urządzenia
        app.get("/api/forecasts/device/{deviceId}", this::getForecastsForDevice);
        
        // Pobieranie prognoz dla budynku
        app.get("/api/forecasts/building/{buildingId}", this::getForecastsForBuilding);
        
        // Retrening modelu dla urządzenia
        app.post("/api/forecasts/retrain/{deviceId}", this::retrainModel);
        
        // Informacje o aktualnym modelu
        app.get("/api/forecasts/model", this::getModelInfo);
        
        // Zarządzanie konfiguracją
        app.get("/api/forecasts/config", this::getConfig);
        app.put("/api/forecasts/config", this::updateConfig);
        
        // Eksport raportów
        app.get("/api/forecasts/export/{deviceId}", this::exportReport);
    }

    /**
     * POST /api/forecasts/generate/{deviceId}
     * Generuje prognozy dla danego urządzenia.
     */
    private void generateForecast(Context ctx) {
        try {
            int deviceId = Integer.parseInt(ctx.pathParam("deviceId"));
            
            forecastService.generateForecast(deviceId);
            
            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", true);
            response.put("message", "Prognozy wygenerowane dla urządzenia " + deviceId);
            response.put("deviceId", deviceId);
            
            ctx.status(200);
            ctx.json(response);
            
        } catch (IncompleteDataException e) {
            ctx.status(400);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", e.getMessage());
            error.put("deviceId", e.getDeviceId());
            error.put("reason", e.getReason());
            ctx.json(error);
        } catch (NumberFormatException e) {
            ctx.status(400);
            sendError(ctx, "Nieprawidłowe ID urządzenia");
        } catch (Exception e) {
            ctx.status(500);
            sendError(ctx, "Błąd podczas generowania prognoz: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * GET /api/forecasts/device/{deviceId}?from=YYYY-MM-DDTHH:mm:ss&to=YYYY-MM-DDTHH:mm:ss
     * Pobiera prognozy dla urządzenia w zadanym okresie.
     */
    private void getForecastsForDevice(Context ctx) {
        try {
            int deviceId = Integer.parseInt(ctx.pathParam("deviceId"));
            
            LocalDateTime from = parseDateTime(ctx.queryParam("from"));
            LocalDateTime to = parseDateTime(ctx.queryParam("to"));
            
            if (from == null || to == null) {
                ctx.status(400);
                sendError(ctx, "Parametry 'from' i 'to' są wymagane (format: YYYY-MM-DDTHH:mm:ss)");
                return;
            }
            
            List<Prognoza> forecasts = forecastService.getForecasts(deviceId, from, to);
            
            ArrayNode forecastsArray = objectMapper.createArrayNode();
            for (Prognoza prognoza : forecasts) {
                ObjectNode forecastNode = objectMapper.createObjectNode();
                forecastNode.put("id", prognoza.getId());
                forecastNode.put("deviceId", prognoza.getUrzadzenieId());
                forecastNode.put("buildingId", prognoza.getBudynekId() != null ? prognoza.getBudynekId() : 0);
                forecastNode.put("czasWygenerowania", prognoza.getCzasWygenerowania().toString());
                forecastNode.put("czasPrognozy", prognoza.getCzasPrognozy().toString());
                forecastNode.put("prognozowanaWartosc", prognoza.getPrognozowanaWartosc());
                forecastNode.put("metryka", prognoza.getMetryka());
                forecastsArray.add(forecastNode);
            }
            
            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", true);
            response.put("deviceId", deviceId);
            response.put("count", forecasts.size());
            response.set("forecasts", forecastsArray);
            
            ctx.status(200);
            ctx.json(response);
            
        } catch (NumberFormatException e) {
            ctx.status(400);
            sendError(ctx, "Nieprawidłowe ID urządzenia");
        } catch (DateTimeParseException e) {
            ctx.status(400);
            sendError(ctx, "Nieprawidłowy format daty. Użyj: YYYY-MM-DDTHH:mm:ss");
        } catch (Exception e) {
            ctx.status(500);
            sendError(ctx, "Błąd podczas pobierania prognoz: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * GET /api/forecasts/building/{buildingId}?from=YYYY-MM-DDTHH:mm:ss&to=YYYY-MM-DDTHH:mm:ss
     * Pobiera prognozy dla budynku w zadanym okresie.
     */
    private void getForecastsForBuilding(Context ctx) {
        try {
            int buildingId = Integer.parseInt(ctx.pathParam("buildingId"));
            
            LocalDateTime from = parseDateTime(ctx.queryParam("from"));
            LocalDateTime to = parseDateTime(ctx.queryParam("to"));
            
            if (from == null || to == null) {
                ctx.status(400);
                sendError(ctx, "Parametry 'from' i 'to' są wymagane (format: YYYY-MM-DDTHH:mm:ss)");
                return;
            }
            
            List<Prognoza> forecasts = forecastService.getForecastsForBuilding(buildingId, from, to);
            
            ArrayNode forecastsArray = objectMapper.createArrayNode();
            for (Prognoza prognoza : forecasts) {
                ObjectNode forecastNode = objectMapper.createObjectNode();
                forecastNode.put("id", prognoza.getId());
                forecastNode.put("deviceId", prognoza.getUrzadzenieId() != null ? prognoza.getUrzadzenieId() : 0);
                forecastNode.put("buildingId", prognoza.getBudynekId());
                forecastNode.put("czasWygenerowania", prognoza.getCzasWygenerowania().toString());
                forecastNode.put("czasPrognozy", prognoza.getCzasPrognozy().toString());
                forecastNode.put("prognozowanaWartosc", prognoza.getPrognozowanaWartosc());
                forecastNode.put("metryka", prognoza.getMetryka());
                forecastsArray.add(forecastNode);
            }
            
            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", true);
            response.put("buildingId", buildingId);
            response.put("count", forecasts.size());
            response.set("forecasts", forecastsArray);
            
            ctx.status(200);
            ctx.json(response);
            
        } catch (NumberFormatException e) {
            ctx.status(400);
            sendError(ctx, "Nieprawidłowe ID budynku");
        } catch (DateTimeParseException e) {
            ctx.status(400);
            sendError(ctx, "Nieprawidłowy format daty. Użyj: YYYY-MM-DDTHH:mm:ss");
        } catch (Exception e) {
            ctx.status(500);
            sendError(ctx, "Błąd podczas pobierania prognoz: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * POST /api/forecasts/retrain/{deviceId}
     * Wymusza retrening modelu dla urządzenia.
     */
    private void retrainModel(Context ctx) {
        try {
            int deviceId = Integer.parseInt(ctx.pathParam("deviceId"));
            
            forecastService.retrainModel(deviceId);
            
            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", true);
            response.put("message", "Retrening modelu rozpoczęty dla urządzenia " + deviceId);
            response.put("deviceId", deviceId);
            
            ctx.status(200);
            ctx.json(response);
            
        } catch (IncompleteDataException e) {
            ctx.status(400);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", e.getMessage());
            error.put("deviceId", e.getDeviceId());
            error.put("reason", e.getReason());
            ctx.json(error);
        } catch (NumberFormatException e) {
            ctx.status(400);
            sendError(ctx, "Nieprawidłowe ID urządzenia");
        } catch (Exception e) {
            ctx.status(500);
            sendError(ctx, "Błąd podczas retreningu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * GET /api/forecasts/model
     * Zwraca informacje o aktualnym wytrenowanym modelu.
     */
    private void getModelInfo(Context ctx) {
        try {
            ForecastModel model = forecastService.getCurrentModel();
            
            ObjectNode response = objectMapper.createObjectNode();
            
            if (model != null && model.isTrained()) {
                response.put("trained", true);
                response.put("baselineConsumption", model.getBaselineConsumption());
                response.put("trainingTimestamp", 
                    model.getTrainingTimestamp() != null ? model.getTrainingTimestamp().toString() : "N/A");
                response.put("trainingSamplesCount", model.getTrainingSamplesCount());
                
                if (model.getMetrics() != null) {
                    ObjectNode metricsNode = objectMapper.createObjectNode();
                    metricsNode.put("mape", model.getMetrics().getMape() * 100);
                    metricsNode.put("mae", model.getMetrics().getMae());
                    metricsNode.put("rmse", model.getMetrics().getRmse());
                    metricsNode.put("sampleCount", model.getMetrics().getSampleCount());
                    response.set("metrics", metricsNode);
                }
            } else {
                response.put("trained", false);
                response.put("message", "Model nie jest jeszcze wytrenowany");
            }
            
            ctx.status(200);
            ctx.json(response);
            
        } catch (Exception e) {
            ctx.status(500);
            sendError(ctx, "Błąd podczas pobierania informacji o modelu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Pomocnicza metoda do parsowania daty z query parametru.
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new DateTimeParseException("Nieprawidłowy format daty: " + dateTimeStr, dateTimeStr, 0);
        }
    }

    private void getConfig(Context ctx) {
        try {
            ConfigManager config = ConfigManager.getInstance();
            
            ObjectNode response = objectMapper.createObjectNode();
            
            ObjectNode modelParams = objectMapper.createObjectNode();
            modelParams.put("minAccuracyThreshold", config.getMinAccuracyThreshold());
            modelParams.put("trainValidationSplit", config.getTrainValidationSplit());
            modelParams.put("minTrainingSamples", config.getMinTrainingSamples());
            response.set("modelParameters", modelParams);
            
            ObjectNode processParams = objectMapper.createObjectNode();
            processParams.put("defaultForecastHorizon", config.getDefaultForecastHorizon());
            processParams.put("historicalDataDays", config.getHistoricalDataDays());
            processParams.put("enableAutoRetraining", config.isEnableAutoRetraining());
            processParams.put("retrainingIntervalHours", config.getRetrainingIntervalHours());
            response.set("processParameters", processParams);
            
            ObjectNode preprocessParams = objectMapper.createObjectNode();
            preprocessParams.put("enableNormalization", config.isEnableNormalization());
            preprocessParams.put("enableOutlierDetection", config.isEnableOutlierDetection());
            response.set("preprocessingParameters", preprocessParams);
            
            ObjectNode weatherParams = objectMapper.createObjectNode();
            weatherParams.put("temperatureThreshold", config.getTemperatureThreshold());
            weatherParams.put("coldWeatherFactor", config.getColdWeatherFactor());
            weatherParams.put("warmWeatherFactor", config.getWarmWeatherFactor());
            response.set("weatherParameters", weatherParams);
            
            ctx.status(200);
            ctx.json(response);
            
        } catch (Exception e) {
            ctx.status(500);
            sendError(ctx, "Błąd podczas pobierania konfiguracji: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updateConfig(Context ctx) {
        try {
            ConfigManager config = ConfigManager.getInstance();
            ObjectNode requestBody = objectMapper.readValue(ctx.body(), ObjectNode.class);
            
            if (requestBody.has("modelParameters")) {
                ObjectNode modelParams = (ObjectNode) requestBody.get("modelParameters");
                if (modelParams.has("minAccuracyThreshold")) {
                    config.setMinAccuracyThreshold(modelParams.get("minAccuracyThreshold").asDouble());
                }
                if (modelParams.has("trainValidationSplit")) {
                    config.setTrainValidationSplit(modelParams.get("trainValidationSplit").asDouble());
                }
                if (modelParams.has("minTrainingSamples")) {
                    config.setMinTrainingSamples(modelParams.get("minTrainingSamples").asInt());
                }
            }
            
            if (requestBody.has("processParameters")) {
                ObjectNode processParams = (ObjectNode) requestBody.get("processParameters");
                if (processParams.has("defaultForecastHorizon")) {
                    config.setDefaultForecastHorizon(processParams.get("defaultForecastHorizon").asInt());
                }
                if (processParams.has("historicalDataDays")) {
                    config.setHistoricalDataDays(processParams.get("historicalDataDays").asInt());
                }
                if (processParams.has("enableAutoRetraining")) {
                    config.setEnableAutoRetraining(processParams.get("enableAutoRetraining").asBoolean());
                }
                if (processParams.has("retrainingIntervalHours")) {
                    config.setRetrainingIntervalHours(processParams.get("retrainingIntervalHours").asInt());
                }
            }
            
            if (requestBody.has("preprocessingParameters")) {
                ObjectNode preprocessParams = (ObjectNode) requestBody.get("preprocessingParameters");
                if (preprocessParams.has("enableNormalization")) {
                    config.setEnableNormalization(preprocessParams.get("enableNormalization").asBoolean());
                }
                if (preprocessParams.has("enableOutlierDetection")) {
                    config.setEnableOutlierDetection(preprocessParams.get("enableOutlierDetection").asBoolean());
                }
            }
            
            if (requestBody.has("weatherParameters")) {
                ObjectNode weatherParams = (ObjectNode) requestBody.get("weatherParameters");
                if (weatherParams.has("temperatureThreshold")) {
                    config.setTemperatureThreshold(weatherParams.get("temperatureThreshold").asDouble());
                }
                if (weatherParams.has("coldWeatherFactor")) {
                    config.setColdWeatherFactor(weatherParams.get("coldWeatherFactor").asDouble());
                }
                if (weatherParams.has("warmWeatherFactor")) {
                    config.setWarmWeatherFactor(weatherParams.get("warmWeatherFactor").asDouble());
                }
            }
            
            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", true);
            response.put("message", "Konfiguracja zaktualizowana");
            
            ctx.status(200);
            ctx.json(response);
            
        } catch (IllegalArgumentException e) {
            ctx.status(400);
            sendError(ctx, "Nieprawidłowa wartość parametru: " + e.getMessage());
        } catch (Exception e) {
            ctx.status(500);
            sendError(ctx, "Błąd podczas aktualizacji konfiguracji: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void exportReport(Context ctx) {
        try {
            int deviceId = Integer.parseInt(ctx.pathParam("deviceId"));
            
            LocalDateTime from = parseDateTime(ctx.queryParam("from"));
            LocalDateTime to = parseDateTime(ctx.queryParam("to"));
            
            if (from == null || to == null) {
                ctx.status(400);
                sendError(ctx, "Parametry 'from' i 'to' są wymagane (format: YYYY-MM-DDTHH:mm:ss)");
                return;
            }
            
            List<Prognoza> forecasts = forecastService.getForecasts(deviceId, from, to);
            
            if (forecasts.isEmpty()) {
                ctx.status(404);
                sendError(ctx, "Brak prognoz dla urządzenia " + deviceId + " w podanym okresie");
                return;
            }
            
            ForecastReportExporter exporter = new ForecastReportExporter();
            String filename = exporter.exportToJSON(forecasts, deviceId, from, to);
            
            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", true);
            response.put("message", "Raport wygenerowany pomyślnie");
            response.put("filename", filename);
            response.put("format", "json");
            response.put("forecastCount", forecasts.size());
            
            ctx.status(200);
            ctx.json(response);
            
        } catch (NumberFormatException e) {
            ctx.status(400);
            sendError(ctx, "Nieprawidłowe ID urządzenia");
        } catch (DateTimeParseException e) {
            ctx.status(400);
            sendError(ctx, "Nieprawidłowy format daty. Użyj: YYYY-MM-DDTHH:mm:ss");
        } catch (IOException e) {
            ctx.status(500);
            sendError(ctx, "Błąd podczas eksportu raportu: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            ctx.status(500);
            sendError(ctx, "Błąd podczas eksportu raportu: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void sendError(Context ctx, String errorMessage) {
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", errorMessage);
        ctx.json(error);
    }
}
