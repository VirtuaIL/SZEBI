package org.example;

import org.example.DTO.Odczyt;
import org.example.interfaces.IAcquisitionData;

import java.time.Instant;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Klasa odpowiedzialna za transformację i przesyłanie danych do Bazy Danych.
 * Implementuje mechanizm buforowania danych w przypadku utraty połączenia.
 */
public class DataCollector {
    private final IAcquisitionData dataStorage;
    private final Queue<Odczyt> buffer = new LinkedList<>();

    public DataCollector(IAcquisitionData dataStorage) {
        this.dataStorage = dataStorage;
    }

    /**
     * Przetwarza mapę urządzeń i wartości na format DTO wymagany przez bazę danych.
     * Każdy rekord jest wzbogacany o informację o zużyciu energii.
     *
     * @param data Mapa obiektów Device i ich aktualnych odczytów.
     */
    public void saveDataBatch(Map<Device, Double> data) {
        if (data.isEmpty()) return;

        for (Map.Entry<Device, Double> entry : data.entrySet()) {
            Device device = entry.getKey();
            Double value = entry.getValue();

            Odczyt reading = new Odczyt();
            reading.setUrzadzenieId(parseId(device.getId()));
            reading.setCzasOdczytu(Instant.now());

            // Budowanie JSON z pomiarem i zużyciem energii
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{ ");
            if (!device.getMetricLabel().equals("zuzycie_energii_W")) {
                jsonBuilder.append("\"").append(device.getMetricLabel()).append("\": ")
                        .append(value).append(", ");
            }
            double powerConsumption = device.getMetricLabel().equals("zuzycie_energii_W")
                    ? value
                    : device.readCurrentPowerUsage();

            jsonBuilder.append("\"zuzycie_energii_W\": ")
                    .append(String.format("%.2f", powerConsumption).replace(',', '.'));
            jsonBuilder.append(" }");

            reading.setPomiary(jsonBuilder.toString());

            try {
                this.dataStorage.saveSensorReading(reading);
                processBuffer(); // Próba opróżnienia bufora przy udanym zapisie
            } catch (Exception e) {
                System.err.println("[DataCollector] Błąd zapisu do DB. Buforowanie danych.");
                buffer.add(reading);
            }
        }
    }

    private void processBuffer() {
        while (!buffer.isEmpty()) {
            Odczyt cached = buffer.poll();
            try {
                this.dataStorage.saveSensorReading(cached);
            } catch (Exception e) {
                buffer.add(cached);
                break;
            }
        }
    }

    private int parseId(String stringId) {
        try {
            String numberOnly = stringId.replaceAll("\\D+", "");
            return numberOnly.isEmpty() ? 0 : Integer.parseInt(numberOnly);
        } catch (NumberFormatException e) {
            return 999;
        }
    }
}