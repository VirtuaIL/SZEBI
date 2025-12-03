package org.example.runner;

import org.example.PostgresDataStorage;
import org.example.DataCollector;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Start Aplikacji SZEBI (Test zapisu danych) ---");
// === FAZA KONFIGURACJI ===

        // 1. Stwórz instancję Twojego modułu danych.
        PostgresDataStorage mainDataStorage = new PostgresDataStorage();

        // 2. Stwórz instancję modułu akwizycji, "wstrzykując" mu Twój moduł danych.
        DataCollector dataCollector = new DataCollector(mainDataStorage);


        // === FAZA URUCHOMIENIA (Symulacja Działania) ===

        int testDeviceId = 1; // Będziemy zapisywać dane dla urządzenia o ID = 1

        // Symulujemy, że system otrzymuje nowy odczyt z czujnika i przekazuje go do zapisu.
        dataCollector.processAndSaveReading(testDeviceId, 24.5);

        System.out.println("\n--- Test zakończony. Sprawdź, czy nowy odczyt pojawił się w bazie danych MongoDB. ---");
    }
}