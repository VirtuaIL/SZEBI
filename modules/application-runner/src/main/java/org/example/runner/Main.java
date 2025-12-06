package org.example.runner;

import org.example.*;
import org.example.DTO.UrzadzenieSzczegoly;

import org.example.OptimizationController;
import org.example.AdministratorPreferences;

import java.util.List;

public class Main {
  public static void main(String[] args) {
    System.out.println("=== Start Systemu SZEBI ===\n");

    // 1. Inicjalizacja komponentów zewnętrznych
    System.out.println("[INFO] Inicjalizacja bazy danych...");
    PostgresDataStorage databaseStorage = new PostgresDataStorage();
    IAnalysisService analysisService = new MockAnalysisService();

    // 2. Inicjalizacja komponentów wewnętrznych - Moduł Akwizycji
    System.out.println("[INFO] Inicjalizacja modułu akwizycji danych...");
    DataCollector dataCollector = new DataCollector(databaseStorage);
    DeviceManager deviceManager = new DeviceManager();
    ErrorReporter errorReporter = new ErrorReporter(AnalysisReportAPI.aquisitionService);

    // 3. Wstrzykiwanie zależności
    CollectionService service = new CollectionService(deviceManager,
        dataCollector, errorReporter, AnalysisReportAPI.aquisitionService);
    AcquisitionAPI api = new AcquisitionAPI(service, deviceManager, dataCollector, AnalysisReportAPI.aquisitionService);

    AnalysisReportAPI anal = new AnalysisReportAPI(databaseStorage);
    IDataPersistenceService dataServiceMock = new MockDataPersistenceService();

    // 4. Pobieranie konfiguracji z Bazy Danych
    System.out.println("[INFO] Pobieranie konfiguracji urządzeń z bazy danych...");
    List<UrzadzenieSzczegoly> devicesFromDb = databaseStorage.getActiveDevicesWithDetails();

    if (devicesFromDb.isEmpty()) {
      System.err.println("[WARN] Brak skonfigurowanych urządzeń w bazie danych.");
    } else {
      System.out.println("[INFO] Znaleziono " + devicesFromDb.size() + " urządzeń w bazie.");
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
    System.out.println("[INFO] Zarejestrowano " + api.getAvailableDevices().size() + " urządzeń w module akwizycji.\n");

    // === 5. Inicjalizacja Modułu Optymalizacji ===
    System.out.println("=== Inicjalizacja modułu optymalizacji ===");
    OptimizationController optimizationController = new OptimizationController();

    // 5a. Konfiguracja preferencji administratora
    AdministratorPreferences adminPrefs = new AdministratorPreferences();
    adminPrefs.setPreferredMinTemp(18.0);
    adminPrefs.setPreferredMaxTemp(24.0);
    adminPrefs.setMaxEnergyUsage(1500.0); // 1.5 kW
    adminPrefs.setTimeOpen("08:00");
    adminPrefs.setTimeClose("20:00");
    adminPrefs.setPriorityComfort(7);
    optimizationController.setAdminPreferences(adminPrefs);

    System.out.println("[INFO] Preferencje administratora:");
    System.out.println(
        "  - Temperatura: " + adminPrefs.getPreferredMinTemp() + "°C - " + adminPrefs.getPreferredMaxTemp() + "°C");
    System.out.println("  - Max zużycie energii: " + adminPrefs.getMaxEnergyUsage() + " W");
    System.out.println("  - Priorytet komfortu: " + adminPrefs.getPriorityComfort() + "/10");

    // 5b. Podłączenie serwisów do kontrolera optymalizacji
    optimizationController.setAcquisitionAPI(api);
    optimizationController.setAlertService(databaseStorage);
    optimizationController.setAcquisitionService(databaseStorage);
    optimizationController.setForecastService(databaseStorage);
    optimizationController.setControlService(databaseStorage);
    optimizationController.setAnalyticsService(databaseStorage);

    System.out.println("[INFO] Kontroler optymalizacji skonfigurowany.");

    optimizationController.optimizeBuildingByRooms(1);
    System.out.println("\n=== System SZEBI uruchomiony ===");

    anal.sendDocumentScheme((t -> {
      return t;
    }), dataServiceMock);
  }

}
