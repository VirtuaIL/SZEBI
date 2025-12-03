package org.example;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CollectionService {
    private final DeviceManager deviceManager;

    private final DataCollector dataCollector;

    private final ErrorReporter errorReporter;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public CollectionService(DeviceManager deviceManager, DataCollector dataCollector, ErrorReporter errorReporter) {
        this.deviceManager = deviceManager;
        this.dataCollector = dataCollector; // ZMIANA
        this.errorReporter = errorReporter;
    }

    public void runPeriodicCollectionTask() {
        // Co 5 sekund dla testów
        scheduler.scheduleAtFixedRate(this::collectDataFromAllDevices, 0, 5, TimeUnit.SECONDS);
        System.out.println("[SERVICE] Uruchomiono cykliczne zbieranie danych.");
    }

    private void collectDataFromAllDevices() {
        System.out.println("\n--- Cykl pobierania danych ---");

        Map<Device, Double> currentBatch = new HashMap<>();

        for (Device device : deviceManager.getActiveDevices()) {
            try {
                Double value = device.readSensor();
                // Wstawiamy cały obiekt device jako klucz
                currentBatch.put(device, value);
            } catch (Exception e) {
                if (e.getMessage().contains("walidacji")) {
                    errorReporter.reportDataValidationError(device.getId(), e.getMessage());
                } else {
                    errorReporter.reportCommunicationError(device.getId(), e.getMessage());
                }
            }
        }

        if (!currentBatch.isEmpty()) {
            dataCollector.saveDataBatch(currentBatch);
        }
    }

    public void forceRead() {
        collectDataFromAllDevices();
    }
}