package org.example;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Serwis odpowiedzialny za cykliczne zbieranie danych.
 * Realizuje wymaganie funkcjonalne pobierania danych co najmniej co 1 minutę.
 */
public class CollectionService {
    private final DeviceManager deviceManager;
    private final DataCollector dataCollector;
    private final ErrorReporter errorReporter;
    private final IAnalysisService analysisService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public CollectionService(DeviceManager deviceManager, DataCollector dataCollector, ErrorReporter errorReporter, IAnalysisService analysisService) {
        this.deviceManager = deviceManager;
        this.dataCollector = dataCollector;
        this.errorReporter = errorReporter;
        this.analysisService = analysisService;
    }

    /**
     * Uruchamia harmonogram zadań.
     * Zgodnie z wymaganiami, interwał wynosi 1 minutę.
     */
    public void runPeriodicCollectionTask() {
        // ZMIANA: Ustawiono 1 minutę dla wersji produkcyjnej
        scheduler.scheduleAtFixedRate(this::collectDataFromAllDevices, 0, 1, TimeUnit.MINUTES);
        System.out.println("[SERVICE] Uruchomiono harmonogram akwizycji (interwał: 1 min).");
    }

    /**
     * Wykonuje pojedynczy cykl odczytu ze wszystkich aktywnych urządzeń.
     * Dane są walidowane, a następnie wysyłane do:
     * 1. Modułu Analizy (Push w czasie rzeczywistym).
     * 2. Bazy Danych (Trwały zapis).
     */
    private void collectDataFromAllDevices() {
        Map<Device, Double> currentBatch = new HashMap<>();

        for (Device device : deviceManager.getActiveDevices()) {
            try {
                Double value = device.readSensor();
                currentBatch.put(device, value);
                Double power = device.readCurrentPowerUsage();

                // Push do analizy
                if (analysisService != null) {
                    analysisService.sendSensorUpdate(device.getId(), value, device.getMetricLabel(), power);
                }
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