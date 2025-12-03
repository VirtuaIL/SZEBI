package org.example;

import org.example.DTO.Odczyt;
import org.example.interfaces.IAcquisitionData;

import java.time.Instant;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class DataCollector {

    // Interfejs (połączenie z bazą)
    private final IAcquisitionData dataStorage;

    // Nasz bufor na wypadek awarii
    private final Queue<Odczyt> buffer = new LinkedList<>();

    public DataCollector(IAcquisitionData dataStorage) {
        this.dataStorage = dataStorage;
    }

    /**
     * Ta metoda przetwarza dane z mapy Device -> Wartość.
     * Tworzy rozbudowany JSON zawierający zawsze zużycie energii.
     */
    public void saveDataBatch(Map<Device, Double> data) {
        if (data.isEmpty()) return;

        System.out.println("[DataCollector] Przetwarzanie " + data.size() + " odczytów...");

        for (Map.Entry<Device, Double> entry : data.entrySet()) {
            Device device = entry.getKey();
            Double value = entry.getValue(); // Główny odczyt (np. temperatura)

            Odczyt reading = new Odczyt();

            // 1. ID: Bierzemy z obiektu Device i parsujemy na int (adapter)
            reading.setUrzadzenieId(parseId(device.getId()));

            // 2. Czas
            reading.setCzasOdczytu(Instant.now());

            // 3. POMIARY: Budowanie JSON-a z dwoma polami
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{ ");

            // A. Dodajemy główny pomiar (chyba że to sam licznik energii, wtedy nie dublujemy)
            // Jeśli urządzenie to np. "TEMP-01" (temperatura_C), dodajemy: "temperatura_C": 21.5,
            if (!device.getMetricLabel().equals("zuzycie_energii_W")) {
                jsonBuilder.append("\"").append(device.getMetricLabel()).append("\": ")
                        .append(value).append(", ");
            }

            // B. Dodajemy zużycie energii (OBOWIĄZKOWE DLA KAŻDEGO WPISU)
            // Jeśli to licznik energii, to główna wartość 'value' jest zużyciem.
            // Jeśli to termometr, pobieramy zużycie z metody readCurrentPowerUsage().
            double powerConsumption;
            if (device.getMetricLabel().equals("zuzycie_energii_W")) {
                powerConsumption = value;
            } else {
                powerConsumption = device.readCurrentPowerUsage();
            }

            // Formatujemy do 2 miejsc po przecinku i zamieniamy przecinek na kropkę (dla JSON)
            jsonBuilder.append("\"zuzycie_energii_W\": ")
                    .append(String.format("%.2f", powerConsumption).replace(',', '.'));

            jsonBuilder.append(" }");

            // Ustawiamy gotowy JSON
            reading.setPomiary(jsonBuilder.toString());

            // === PRÓBA ZAPISU DO BAZY ===
            try {
                this.dataStorage.saveSensorReading(reading);
                System.out.println(">> [BAZA] Zapisano dla ID " + reading.getUrzadzenieId() + ": " + jsonBuilder.toString());

                // Przy okazji próbujemy opróżnić bufor
                processBuffer();
            } catch (Exception e) {
                System.err.println("!! [AWARIA BAZY] Błąd zapisu: " + e.getMessage());
                buffer.add(reading); // Trafia do bufora
            }
        }
    }

    private void processBuffer() {
        while (!buffer.isEmpty()) {
            Odczyt cached = buffer.poll();
            try {
                this.dataStorage.saveSensorReading(cached);
                System.out.println(">> [RECOVERY] Wysłano zaległy odczyt z bufora.");
            } catch (Exception e) {
                buffer.add(cached); // Wracamy do kolejki, baza nadal leży
                break;
            }
        }
    }

    // Pomocnicza metoda do wyciągania liczby z ID (np. "TEMP-001" -> 1)
    private int parseId(String stringId) {
        try {
            // Usuwa wszystko co nie jest cyfrą
            String numberOnly = stringId.replaceAll("\\D+", "");
            return numberOnly.isEmpty() ? 0 : Integer.parseInt(numberOnly);
        } catch (NumberFormatException e) {
            return 999; // ID błędu
        }
    }
}