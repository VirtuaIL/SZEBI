package org.example.runner;

import org.example.*;
import org.example.DTO.*;
import org.example.interfaces.*;
import java.util.Collections;
import org.example.OptimizationController;
import org.example.AdministratorPreferences;
import org.example.runner.AuthService;
import org.example.runner.AuthController;
import org.example.runner.AlertsController;
import org.example.runner.DevicesController;

import java.time.LocalDateTime;
import java.util.List;

public class Main {
  public static void main(String[] args) {
    System.out.println("=== Start Systemu SZEBI ===\n");

    // 1. Inicjalizacja komponentów zewnętrznych
    System.out.println("[INFO] Inicjalizacja bazy danych...");
    PostgresDataStorage databaseStorage = new PostgresDataStorage();

    // 2. Inicjalizacja komponentów wewnętrznych - Moduł Akwizycji
    System.out.println("[INFO] Inicjalizacja modułu akwizycji danych...");
    DataCollector dataCollector = new DataCollector(databaseStorage);
    DeviceManager deviceManager = new DeviceManager();
    ErrorReporter errorReporter = new ErrorReporter(AnalysisReportAPI.getAquisitionProxy());

    // 3. Wstrzykiwanie zależności
    CollectionService service = new CollectionService(deviceManager,
        dataCollector, errorReporter, AnalysisReportAPI.getAquisitionProxy());
    AcquisitionAPI api = new AcquisitionAPI(service, deviceManager, dataCollector,
        AnalysisReportAPI.getAquisitionProxy());

    // 3.5. Inicjalizacja modułu Analizy i raportowania
    AnalysisReportAPI anal = new AnalysisReportAPI(databaseStorage);

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
    adminPrefs.setMaxEnergyUsage(1500.0);
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
    optimizationController.setUserService(databaseStorage); // Podłączenie serwisu użytkowników

    // START DEMO SCENARIUSZ
    System.out.println("\n========== DEMONSTRACJA: Preferencje Użytkownika ==========");
    System.out.println("Scenariusz: Admin chce 18-24°C. Użytkownik w pokoju 999 chce poziom komfortu 5 (Najcieplej).");

    // Tworzymy anonimową klasę Symulatora Danych
    class DemoDataSimulator implements IControlData, IUserData {
      public Uzytkownik getUserById(int id) {
        Uzytkownik u = new Uzytkownik();
        u.setId(id);
        u.setImie("Test");
        org.example.DTO.UserPreferences prefs = new org.example.DTO.UserPreferences();
        prefs.setComfortLevel(5);
        u.setPreferencje(prefs);
        return u;
      }

      public Uzytkownik getUserByEmail(String email) {
        return null;
      }

      public org.example.DTO.Rola getRoleById(int id) {
        return null;
      }

      public Uzytkownik saveUser(Uzytkownik u) {
        return u;
      }

      public List<org.example.DTO.Pokoj> getRoomsInBuilding(int bid) {
        org.example.DTO.Pokoj p = new org.example.DTO.Pokoj();
        p.setId(999);
        p.setNumerPokoju("DEMO-ROOM");
        p.setBudynekId(bid);
        p.getUzytkownicyIds().add(100);
        return java.util.Collections.singletonList(p);
      }

      public List<org.example.DTO.Urzadzenie> getDevicesInRoom(int rid) {
        org.example.DTO.Urzadzenie u = new org.example.DTO.Urzadzenie();
        u.setId(555);
        u.setPokojId(rid);
        u.setParametryPracy("{\"temperatura_C\": 20.0, \"set_temp\": 20.0}");
        return java.util.Collections.singletonList(u);
      }

      public org.example.DTO.Urzadzenie getDeviceById(int id) {
        return null;
      }

      public void updateDevice(org.example.DTO.Urzadzenie d) {
        System.out.println("   [DEMO DB] Zaktualizowano urządzenie " + d.getId() + ": " + d.getParametryPracy());
      }

      public boolean isDatabaseConnected() {
        return true;
      }

      public Umowa getActiveContractForBuilding(int buildingId) {
        return null;
      }

      public List<Odczyt> getReadingsForDevice(int deviceId, LocalDateTime from, LocalDateTime to) {
        return Collections.emptyList();
      }
    }

    DemoDataSimulator demoSim = new DemoDataSimulator();

    optimizationController.setControlService(demoSim);
    optimizationController.setUserService(demoSim);

    optimizationController.optimizeEnergyConsumption(1);

    optimizationController.setControlService(databaseStorage);
    optimizationController.setUserService(databaseStorage);
    System.out.println("===========================================================\n");

    optimizationController.optimizeEnergyConsumption(1);
    System.out.println("\n=== System SZEBI uruchomiony ===");

    // === 6. Uruchomienie REST API dla GUI ===
    System.out.println("\n=== Inicjalizacja REST API ===");
    AuthService authService = new AuthService(databaseStorage);
    AuthController authController = new AuthController(authService);
    
    AlertsController alertsController = new AlertsController(databaseStorage);
    
    DevicesController devicesController = new DevicesController(databaseStorage, databaseStorage, api);

    io.javalin.Javalin app = io.javalin.Javalin.create(config -> {
      config.bundledPlugins.enableCors(cors -> {
        cors.addRule(it -> it.anyHost());
      });
    });

    authController.setupRoutes(app);
    alertsController.setupRoutes(app);
    devicesController.setupRoutes(app);

    int apiPort = 8080;
    // Nasłuchuj na wszystkich interfejsach (0.0.0.0) aby umożliwić dostęp z innych
    // urządzeń w sieci
    app.start("0.0.0.0", apiPort);
    System.out.println("[INFO] REST API uruchomione na porcie " + apiPort);
    System.out.println("[INFO] Endpoint logowania: http://localhost:" + apiPort + "/api/login");
    System.out.println("[INFO] Endpoint alarmów: http://localhost:" + apiPort + "/api/alerts");
    System.out.println("[INFO] Endpoint urządzeń: http://localhost:" + apiPort + "/api/devices");
    System.out.println("[INFO] API dostępne również z sieci lokalnej na porcie " + apiPort);
    System.out.println("\n=== System SZEBI w pełni uruchomiony ===");

    // Dalsza część modułu analizy i raportowania
    anal.sendDocumentScheme(AnalysisReportAPI.newReportScheme()
        .setFrom(LocalDateTime.now().minusDays(1))
        .setTo(LocalDateTime.now())
        .includeMetrics(AnalysisReportAPI.getAvailableMetrics()));
  }

}
