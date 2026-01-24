package org.example;

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

public class AcquisitionControllerJavalin {
    private final IAcquisitionData acquisitionData;
    private final IControlData controlData;
    private final AcquisitionAPI acquisitionAPI;
    private final ObjectMapper objectMapper;

    public AcquisitionControllerJavalin(IAcquisitionData acquisitionData, IControlData controlData, AcquisitionAPI acquisitionAPI) {
        this.acquisitionData = acquisitionData;
        this.controlData = controlData;
        this.acquisitionAPI = acquisitionAPI;
        this.objectMapper = new ObjectMapper();
    }

    public void setupRoutes(Javalin app) {

        // Periodic collection task
        app.get("/api/acquisition/startPeriodicCollectionTask", this::startPeriodicCollectionTask);

        // Single read (to DB)
        app.post("/api/devices/{id}/requestSingleRead", this::requestSingleRead);

        // All sensors read (to DB)
        app.post("/api/acquisition/requestAllRead", this::requestAllRead);

        // Create device and add it to list + db
        app.put("/api/acquisition/createDevice", this::createDevice);



    }

    private void startPeriodicCollectionTask(Context ctx) {
        try {
            // Logic
            acquisitionAPI.startPeriodicCollectionTask();

            // Response
            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", "true");
            ctx.status(200);
            ctx.json(response);
        } catch (Exception e) {
            ctx.status(500);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Błąd podczas uruchamiania zadania " + e.getMessage());
            ctx.json(error);
            e.printStackTrace();
        }
    }

    private void requestSingleRead(Context ctx) {
        try {
            // Logic
            int deviceID = ctx.pathParamAsClass("id", Integer.class).get();
            acquisitionAPI.requestSensorRead(Integer.toString(deviceID));

            // Response
            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", "true");
            response.put("message", "pomyślnie zlecono odczyt");
            ctx.status(200);
            ctx.json(response);
        } catch (Exception e) {
            ctx.status(500);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Błąd podczas próby odczytu" + e.getMessage());
            ctx.json(error);
            e.printStackTrace();
        }


    }

    private void requestAllRead(Context ctx) {
        try {
            // Logic
            acquisitionAPI.requestAllData();

            // Response
            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", "true");
            response.put("message", "pomyślnie zlecono odczyt na wszystkich urządzeniach");
            ctx.status(200);
            ctx.json(response);
        } catch (Exception e) {
            ctx.status(500);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Błąd podczas próby odczytu" + e.getMessage());
            ctx.json(error);
            e.printStackTrace();
        }


    }

    private void createDevice(Context ctx) {
        try {
            // Logic
            String deviceName = "empty";
            int roomID = 0;
            int modelID = 0;
            Number minValue = -1;
            Number maxValue = -1;
            String metricLabel = "none";
            Number powerUsage = -1;


            // Parsuj dane z request body
            JsonNode requestBody = objectMapper.readTree(ctx.body());

            // Aktualizuj parametry pracy jeśli podano
            if (requestBody.has("deviceName")) {
                deviceName = requestBody.get("deviceName").asText();
            }
            if (requestBody.has("roomID")) {
                roomID = requestBody.get("roomID").asInt();
            }
            if (requestBody.has("modelID")) {
                modelID = requestBody.get("modelID").asInt();
            }
            if (requestBody.has("minValue")) {
                minValue = requestBody.get("minValue").asDouble();
            }
            if (requestBody.has("maxValue")) {
                maxValue = requestBody.get("maxValue").asDouble();
            }
            if (requestBody.has("metricLabel")) {
                metricLabel = requestBody.get("metricLabel").asText();
            }
            if (requestBody.has("powerUsage")) {
                powerUsage = requestBody.get("powerUsage").asDouble();
            }

            acquisitionAPI.createNewDevice(deviceName, roomID, modelID, minValue, maxValue, metricLabel, powerUsage);
            // Response
            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", "true");
            response.put("message", "pomyślnie utworzono urzadzenie");
            ctx.status(200);
            ctx.json(response);
        } catch (Exception e) {
            ctx.status(500);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Błąd podczas próby odczytu" + e.getMessage());
            ctx.json(error);
            e.printStackTrace();
        }


    }


}
