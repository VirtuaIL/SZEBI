package org.example.runner;

import org.example.*;
import org.example.DTO.UrzadzenieSzczegoly;

import java.util.List;

public class Main {
  public static void main(String[] args) {
    System.out.println("--- Start Systemu SZEBI (Moduł Akwizycji) ---");

    // 1. Inicjalizacja komponentów zewnętrznych
    PostgresDataStorage databaseStorage = new PostgresDataStorage();
    IAnalysisService analysisService = new MockAnalysisService();

    // 2. Inicjalizacja komponentów wewnętrznych
    DataCollector dataCollector = new DataCollector(databaseStorage);
    DeviceManager deviceManager = new DeviceManager();
    ErrorReporter errorReporter = new ErrorReporter(analysisService);

    // 3. Wstrzykiwanie zależności
    CollectionService service = new CollectionService(deviceManager, dataCollector, errorReporter, analysisService);
    AcquisitionAPI api = new AcquisitionAPI(service, deviceManager, dataCollector, analysisService);

    // 4. Pobieranie konfiguracji z Bazy Danych
    System.out.println("[INFO] Pobieranie konfiguracji urządzeń...");
    List<UrzadzenieSzczegoly> devicesFromDb = databaseStorage.getActiveDevicesWithDetails();

    if (devicesFromDb.isEmpty()) {
      System.err.println("[WARN] Brak skonfigurowanych urządzeń w bazie danych.");
    }

    for (UrzadzenieSzczegoly dbDevice : devicesFromDb) {
      String id = String.valueOf(dbDevice.getId());
      String name = (dbDevice.getNazwaProducenta() != null ? dbDevice.getNazwaProducenta() : "") + " " +
          (dbDevice.getNazwaModelu() != null ? dbDevice.getNazwaModelu() : "");

      // Rejestracja w API (obsługuje konwersję typów i wartości domyślne)
      api.registerNewDevice(
          id,
          name.trim(),
          dbDevice.getMinRange(),
          dbDevice.getMaxRange(),
          dbDevice.getMetricLabel(),
          dbDevice.getMocW());
    }
    System.out.println("[INFO] Zarejestrowano " + api.getAvailableDevices().size() + " urządzeń.");

    // 5. Uruchomienie procesu akwizycji
    service.runPeriodicCollectionTask();
  }
}
