package org.example.runner;

import org.example.*;
import org.example.DTO.Urzadzenie;
import org.example.DTO.UrzadzenieSzczegoly;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Start Aplikacji SZEBI (Pełna Integracja) ---");

        // 1. BAZA DANYCH (To pochodzi od kolegi)
        PostgresDataStorage databaseStorage = new PostgresDataStorage();

        // 2. DATA COLLECTOR (Most między bazą a Twoim systemem)
        // Tworzymy go tak jak chciał kolega (podając bazę),
        // ale w środku ma już naszą logikę z Kroku 1.
        DataCollector dataCollector = new DataCollector(databaseStorage);

        List<UrzadzenieSzczegoly> devices = databaseStorage.getActiveDevicesWithDetails();
        System.out.println("Aktywne urządzenia w systemie:");
        for (UrzadzenieSzczegoly device : devices) {
            System.out.println("ID: " + device.getId() +
                    ", Nazwa Typu: " + device.getNazwaTypu() +
                    ", Nazwa producenta: " + device.getNazwaProducenta() +
                    ", Nazwa modelu: " + device.getNazwaModelu() +
                    ", Nazwa pokoju: " + device.getNazwaPokoju() +
                    ", Min zakres: " + device.getMinRange() +
                    ", Max zakres: " + device.getMaxRange() +
                    ", Etykieta metryki: " + device.getMetricLabel() +
                    ", Moc (W): " + device.getMocW() +
                    ", Ściemnialna: " + device.getSciemnialna() +
                    ", Barwa (K): " + device.getBarwaK()

            );
        }




        // 3. RESZTA TWOJEGO SYSTEMU
//        DeviceManager deviceManager = new DeviceManager();
//        MockAnalysisService analysisService = new MockAnalysisService();
//        ErrorReporter errorReporter = new ErrorReporter(analysisService);

        // Wstrzykujemy dataCollector do Twojego serwisu
//        CollectionService service = new CollectionService(deviceManager, dataCollector, errorReporter);
//        AcquisitionAPI api = new AcquisitionAPI(service, deviceManager);

        // 4. KONFIGURACJA URZĄDZEŃ
//        System.out.println("[SZEBI] Konfiguracja sensorów...");

        // TERAZ PODAJEMY 5 ARGUMENTÓW: ID, Nazwa, Min, Max, ETYKIETA JSON

        // Żeby pasowało do zrzutu ekranu z bazy:
//        api.registerNewDevice("SENSOR-1", "Czujnik Salon", 18.0, 26.0, "temperatura_C");

//        api.registerNewDevice("SENSOR-2", "Wilgotność Łazienka", 30.0, 90.0, "wilgotnosc_procent");

//        api.registerNewDevice("SENSOR-100", "Licznik Główny", 0.0, 5000.0, "zuzycie_energii_W");

//        service.runPeriodicCollectionTask();

        System.out.println("[SZEBI] System uruchomiony. Czekam na cykle odczytu...");
    }
}