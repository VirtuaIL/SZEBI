package org.example;

import org.example.interfaces.IAcquisitionData;
import org.example.DTO.Odczyt;

import java.time.Instant;

public class DataCollector {

    private final IAcquisitionData dataStorage;

    public DataCollector(IAcquisitionData dataStorage) {
        this.dataStorage = dataStorage;
    }

    public void processAndSaveReading(int deviceId, double temperature) {

        System.out.println("\n[Akwizycja] Otrzymano odczyt temperatury z czujnika ID: " + deviceId);

        Odczyt newReading = new Odczyt();
        newReading.setUrzadzenieId(deviceId);
        newReading.setCzasOdczytu(Instant.now());
        newReading.setPomiary("{ \"temperatura_C\": " + temperature + " }");

        // Wywołanie metody z interfejsu udostępnionego przez Twój moduł bazy danych.
        this.dataStorage.saveSensorReading(newReading);

        System.out.println("[Akwizycja] Dane zostały przekazane do zapisu.");
    }
}
