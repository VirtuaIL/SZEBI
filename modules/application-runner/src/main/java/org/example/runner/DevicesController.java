package org.example.runner;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.example.interfaces.IAcquisitionData;
import org.example.interfaces.IControlData;
import org.example.DTO.UrzadzenieSzczegoly;
import org.example.DTO.Urzadzenie;
import org.example.DTO.Odczyt;
import org.example.AcquisitionAPI;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.List;

public class DevicesController {
    private final IAcquisitionData acquisitionData;
    private final IControlData controlData;
    private final AcquisitionAPI acquisitionAPI;
    private final ObjectMapper objectMapper;

    public DevicesController(IAcquisitionData acquisitionData, IControlData controlData, AcquisitionAPI acquisitionAPI) {
        this.acquisitionData = acquisitionData;
        this.controlData = controlData;
        this.acquisitionAPI = acquisitionAPI;
        this.objectMapper = new ObjectMapper();
    }

    public void setupRoutes(Javalin app) {
        // Lista wszystkich aktywnych urządzeń
        app.get("/api/devices", this::getAllDevices);
        
        // Szczegóły pojedynczego urządzenia
        app.get("/api/devices/{id}", this::getDeviceById);
        
        // Historia odczytów urządzenia
        app.get("/api/devices/{id}/readings", this::getDeviceReadings);
        
        // Aktualny odczyt z urządzenia (wymuszenie odczytu)
        app.post("/api/devices/{id}/read", this::readDeviceSensor);
        
        // Aktualizacja urządzenia
        app.put("/api/devices/{id}", this::updateDevice);
        
        // Rejestracja nowego urządzenia
        app.post("/api/devices", this::registerDevice);
    }

    private void getAllDevices(Context ctx) {
        try {
            List<UrzadzenieSzczegoly> devices = acquisitionData.getActiveDevicesWithDetails();
            
            ArrayNode devicesArray = objectMapper.createArrayNode();
            for (UrzadzenieSzczegoly device : devices) {
                devicesArray.add(convertDeviceToJson(device));
            }
            
            ctx.status(200);
            ctx.json(devicesArray);
        } catch (Exception e) {
            ctx.status(500);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Błąd podczas pobierania urządzeń: " + e.getMessage());
            ctx.json(error);
            e.printStackTrace();
        }
    }

    private void getDeviceById(Context ctx) {
        try {
            int deviceId = ctx.pathParamAsClass("id", Integer.class).get();
            
            Urzadzenie device = controlData.getDeviceById(deviceId);
            
            if (device == null) {
                ctx.status(404);
                ObjectNode error = objectMapper.createObjectNode();
                error.put("error", "Urządzenie o ID " + deviceId + " nie zostało znalezione");
                ctx.json(error);
                return;
            }
            
            // Spróbuj pobrać pełne szczegóły
            List<UrzadzenieSzczegoly> allDevices = acquisitionData.getActiveDevicesWithDetails();
            UrzadzenieSzczegoly deviceDetails = allDevices.stream()
                .filter(d -> d.getId() == deviceId)
                .findFirst()
                .orElse(null);
            
            if (deviceDetails != null) {
                ctx.status(200);
                ctx.json(convertDeviceToJson(deviceDetails));
            } else {
                // Jeśli nie ma w aktywnych, zwróć podstawowe dane
                ObjectNode deviceJson = objectMapper.createObjectNode();
                deviceJson.put("id", device.getId());
                deviceJson.put("pokojId", device.getPokojId());
                deviceJson.put("aktywny", device.isAktywny());
                ctx.status(200);
                ctx.json(deviceJson);
            }
        } catch (Exception e) {
            ctx.status(500);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Błąd podczas pobierania urządzenia: " + e.getMessage());
            ctx.json(error);
            e.printStackTrace();
        }
    }

    private void getDeviceReadings(Context ctx) {
        try {
            int deviceId = ctx.pathParamAsClass("id", Integer.class).get();
            
            // Domyślnie ostatnie 24h, ale można podać parametry
            LocalDateTime to = LocalDateTime.now();
            LocalDateTime from = to.minusHours(24);
            
            String fromParam = ctx.queryParam("from");
            String toParam = ctx.queryParam("to");
            
            if (fromParam != null) {
                from = LocalDateTime.parse(fromParam, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            if (toParam != null) {
                to = LocalDateTime.parse(toParam, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            
            List<Odczyt> readings = controlData.getReadingsForDevice(deviceId, from, to);
            
            ArrayNode readingsArray = objectMapper.createArrayNode();
            for (Odczyt reading : readings) {
                readingsArray.add(convertReadingToJson(reading));
            }
            
            ctx.status(200);
            ctx.json(readingsArray);
        } catch (Exception e) {
            ctx.status(500);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Błąd podczas pobierania odczytów: " + e.getMessage());
            ctx.json(error);
            e.printStackTrace();
        }
    }

    private void readDeviceSensor(Context ctx) {
        try {
            int deviceId = ctx.pathParamAsClass("id", Integer.class).get();
            
            // Wymuś odczyt z urządzenia
            Double value = acquisitionAPI.requestSensorRead(String.valueOf(deviceId));
            
            if (value == null) {
                ctx.status(404);
                ObjectNode error = objectMapper.createObjectNode();
                error.put("error", "Nie można odczytać wartości z urządzenia " + deviceId);
                ctx.json(error);
                return;
            }
            
            ObjectNode response = objectMapper.createObjectNode();
            response.put("deviceId", deviceId);
            response.put("value", value);
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            ctx.status(200);
            ctx.json(response);
        } catch (Exception e) {
            ctx.status(500);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Błąd podczas odczytu urządzenia: " + e.getMessage());
            ctx.json(error);
            e.printStackTrace();
        }
    }

    private void updateDevice(Context ctx) {
        try {
            int deviceId = ctx.pathParamAsClass("id", Integer.class).get();
            
            // Pobierz istniejące urządzenie
            Urzadzenie device = controlData.getDeviceById(deviceId);
            if (device == null) {
                ctx.status(404);
                ObjectNode error = objectMapper.createObjectNode();
                error.put("error", "Urządzenie o ID " + deviceId + " nie zostało znalezione");
                ctx.json(error);
                return;
            }
            
            // Parsuj dane z request body
            JsonNode requestBody = objectMapper.readTree(ctx.body());
            
            // Aktualizuj parametry pracy jeśli podano
            if (requestBody.has("parametryPracy")) {
                device.setParametryPracy(requestBody.get("parametryPracy").asText());
            }
            
            if (requestBody.has("aktywny")) {
                device.setAktywny(requestBody.get("aktywny").asBoolean());
            }
            
            // Zaktualizuj urządzenie w bazie
            controlData.updateDevice(device);
            
            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", true);
            response.put("message", "Urządzenie zostało zaktualizowane");
            response.put("id", device.getId());
            
            ctx.status(200);
            ctx.json(response);
        } catch (Exception e) {
            ctx.status(500);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Błąd podczas aktualizacji urządzenia: " + e.getMessage());
            ctx.json(error);
            e.printStackTrace();
        }
    }

    private void registerDevice(Context ctx) {
        try {
            JsonNode requestBody = objectMapper.readTree(ctx.body());
            
            String id = requestBody.has("deviceId") ? requestBody.get("deviceId").asText() : null;
            String name = requestBody.has("name") ? requestBody.get("name").asText() : null;
            Number min = requestBody.has("minRange") ? requestBody.get("minRange").asDouble() : null;
            Number max = requestBody.has("maxRange") ? requestBody.get("maxRange").asDouble() : null;
            String metricLabel = requestBody.has("metricLabel") ? requestBody.get("metricLabel").asText() : null;
            Number powerUsage = requestBody.has("powerW") ? requestBody.get("powerW").asDouble() : null;
            // roomId będzie używany w przyszłości do zapisu urządzenia do bazy danych
            // Integer roomId = requestBody.has("roomId") ? requestBody.get("roomId").asInt() : null;
            
            // Rejestruj w AcquisitionAPI
            acquisitionAPI.registerNewDevice(id, name, min, max, metricLabel, powerUsage);
            
            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", true);
            response.put("message", "Urządzenie zostało zarejestrowane");
            if (id != null) {
                response.put("deviceId", id);
            }
            
            ctx.status(201);
            ctx.json(response);
        } catch (Exception e) {
            ctx.status(500);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Błąd podczas rejestracji urządzenia: " + e.getMessage());
            ctx.json(error);
            e.printStackTrace();
        }
    }

    private ObjectNode convertDeviceToJson(UrzadzenieSzczegoly device) {
        ObjectNode deviceJson = objectMapper.createObjectNode();
        deviceJson.put("id", device.getId());
        deviceJson.put("pokojId", device.getPokojId());
        deviceJson.put("aktywny", device.isAktywny());
        
        // Typ urządzenia
        String type = device.getNazwaTypu();
        if (type == null && device.getNazwaModelu() != null) {
            type = device.getNazwaModelu();
        }
        deviceJson.put("type", type != null ? type : "Nieznany typ");
        
        // Lokalizacja
        String location = device.getNazwaPokoju() != null ? device.getNazwaPokoju() : "Nieznana lokalizacja";
        deviceJson.put("location", location);
        
        // Status - używamy informacji z bazy danych, nie odczytujemy wartości (można później dodać cache)
        // Wartość będzie odczytywana na żądanie przez endpoint POST /api/devices/{id}/read
        String status = device.isAktywny() ? "DZIAŁA" : "NIEAKTYWNE";
        String currentValue = "Kliknij 'Odśwież' aby odczytać";
        
        deviceJson.put("status", status);
        deviceJson.put("currentValue", currentValue);
        deviceJson.put("lastUpdate", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // Dodatkowe informacje
        if (device.getNazwaProducenta() != null) {
            deviceJson.put("producer", device.getNazwaProducenta());
        }
        if (device.getNazwaModelu() != null) {
            deviceJson.put("model", device.getNazwaModelu());
        }
        if (device.getMetricLabel() != null) {
            deviceJson.put("metricLabel", device.getMetricLabel());
        }
        if (device.getMinRange() != null) {
            deviceJson.put("minRange", device.getMinRange());
        }
        if (device.getMaxRange() != null) {
            deviceJson.put("maxRange", device.getMaxRange());
        }
        
        return deviceJson;
    }

    private ObjectNode convertReadingToJson(Odczyt reading) {
        ObjectNode readingJson = objectMapper.createObjectNode();
        readingJson.put("deviceId", reading.getUrzadzenieId());
        
        // Konwertuj Instant na LocalDateTime i formatuj
        LocalDateTime dateTime = LocalDateTime.ofInstant(reading.getCzasOdczytu(), ZoneId.systemDefault());
        readingJson.put("timestamp", dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        readingJson.put("time", dateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        
        // Parsuj JSON pomiarów
        if (reading.getPomiary() != null && !reading.getPomiary().isEmpty()) {
            try {
                JsonNode pomiaryNode = objectMapper.readTree(reading.getPomiary());
                
                // Spróbuj wyodrębnić wartość z pomiarów
                // Może być w różnych formatach: wartosc, temperatura_C, jasnosc_procent, itp.
                double value = 0.0;
                boolean found = false;
                
                if (pomiaryNode.isArray() && pomiaryNode.size() > 0) {
                    // Jeśli pomiary to tablica, weź pierwszą wartość
                    value = pomiaryNode.get(0).asDouble();
                    found = true;
                } else if (pomiaryNode.isObject()) {
                    // Szukaj wartości w różnych możliwych kluczach
                    String[] possibleKeys = {"wartosc", "value", "temperatura_C", "jasnosc_procent", "temperature", "brightness"};
                    for (String key : possibleKeys) {
                        if (pomiaryNode.has(key)) {
                            value = pomiaryNode.get(key).asDouble();
                            found = true;
                            break;
                        }
                    }
                }
                
                if (found) {
                    readingJson.put("value", value);
                } else {
                    // Jeśli nie znaleziono wartości, zapisz cały JSON
                    readingJson.put("value", 0.0);
                    readingJson.set("rawMeasurements", pomiaryNode);
                }
            } catch (Exception e) {
                readingJson.put("value", 0.0);
                readingJson.put("error", "Nie można sparsować pomiarów");
            }
        } else {
            readingJson.put("value", 0.0);
        }
        
        return readingJson;
    }
}
