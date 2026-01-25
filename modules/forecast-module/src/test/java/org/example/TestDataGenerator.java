package org.example;

import org.example.DTO.Odczyt;
import org.example.PostgresDataStorage;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestDataGenerator {
    
    private final PostgresDataStorage database;
    private final Random random;
    
    public TestDataGenerator() {
        this.database = new PostgresDataStorage();
        this.random = new Random();
    }
    
    public void generateTestData(int deviceId, int daysBack, int readingsPerDay) {
        System.out.println("Generowanie danych dla urządzenia " + deviceId + "...");
        
        List<Odczyt> readings = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int day = 0; day < daysBack; day++) {
            LocalDateTime dayStart = now.minusDays(day).withHour(0).withMinute(0).withSecond(0).withNano(0);
            
            for (int i = 0; i < readingsPerDay; i++) {
                int minutesOffset = (24 * 60 / readingsPerDay) * i;
                LocalDateTime readingTime = dayStart.plusMinutes(minutesOffset);
                
                Odczyt reading = createTestReading(deviceId, readingTime);
                readings.add(reading);
            }
        }
        
        System.out.println("Próba zapisania " + readings.size() + " odczytów do MongoDB...");
        try {
            database.saveBatchSensorReadings(readings);
            System.out.println("✓ Zapisano " + readings.size() + " odczytów dla urządzenia " + deviceId);
        } catch (Exception e) {
            System.err.println("✗ Błąd podczas zapisywania: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private Odczyt createTestReading(int deviceId, LocalDateTime time) {
        Odczyt reading = new Odczyt();
        reading.setUrzadzenieId(deviceId);
        
        Instant instant = time.toInstant(ZoneOffset.UTC);
        reading.setCzasOdczytu(instant);
        
        String pomiaryJson;
        
        switch (deviceId) {
            case 1:
                double temp = 18.0 + random.nextDouble() * 8.0;
                double power = 0.1 + random.nextDouble() * 0.1;
                pomiaryJson = String.format(
                    "{\"temperatura_C\": %.2f, \"zuzycie_energii_W\": %.2f}",
                    temp, power
                );
                break;
                
            case 2:
                double humidity = 40.0 + random.nextDouble() * 20.0;
                double power2 = 0.15 + random.nextDouble() * 0.15;
                pomiaryJson = String.format(
                    "{\"wilgotnosc_procent\": %.2f, \"zuzycie_energii_W\": %.2f}",
                    humidity, power2
                );
                break;
                
            case 3:
                boolean isOn = random.nextDouble() > 0.3;
                int brightness = isOn ? (50 + random.nextInt(50)) : 0;
                double power3 = isOn ? (5.0 + random.nextDouble() * 5.0) : 0.1;
                pomiaryJson = String.format(
                    "{\"stan\": \"%s\", \"jasnosc_procent\": %d, \"zuzycie_energii_W\": %.2f}",
                    isOn ? "wlaczona" : "wylaczona", brightness, power3
                );
                break;
                
            default:
                pomiaryJson = "{\"wartosc\": " + (random.nextDouble() * 100) + "}";
        }
        
        reading.setPomiary(pomiaryJson);
        return reading;
    }
    
    public void generateAllTestData(int days, int readingsPerDay) {
        System.out.println("=== GENEROWANIE DANYCH TESTOWYCH ===");
        System.out.println("Dni wstecz: " + days);
        System.out.println("Odczyty na dzień: " + readingsPerDay);
        System.out.println();
        
        for (int deviceId = 1; deviceId <= 3; deviceId++) {
            generateTestData(deviceId, days, readingsPerDay);
        }
        
        System.out.println("\n=== ZAKOŃCZONO GENEROWANIE DANYCH ===");
    }
    
    public static void main(String[] args) {
        System.out.println("=== TEST DATA GENERATOR ===");
        System.out.println("Sprawdzanie połączenia z MongoDB...");
        
        TestDataGenerator generator = new TestDataGenerator();
        
        generator.generateAllTestData(7, 24);
        
        System.out.println("\n=== WERYFIKACJA ===");
        System.out.println("Sprawdzanie czy dane zostały zapisane...");
        
        PostgresDataStorage db = new PostgresDataStorage();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime weekAgo = now.minusDays(7);
        
        for (int deviceId = 1; deviceId <= 3; deviceId++) {
            var readings = db.getReadingsForDevice(deviceId, weekAgo, now);
            System.out.println("Urządzenie " + deviceId + ": znaleziono " + readings.size() + " odczytów");
        }
        
        System.out.println("\nMożesz teraz uruchomić ForecastingModule.main() aby przetestować prognozowanie!");
    }
}
