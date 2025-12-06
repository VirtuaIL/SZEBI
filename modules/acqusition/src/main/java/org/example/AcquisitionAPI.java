package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Główne API Modułu Akwizycji Danych.
 * <p>
 * Klasa ta stanowi fasadę udostępniającą funkcjonalności modułu
 * dla innych komponentów systemu.
 * Realizuje wymagania funkcjonalne dotyczące odczytu danych na żądanie
 * oraz udostępniania listy aktywnych urządzeń.
 * </p>
 */
public class AcquisitionAPI {
  private final CollectionService collectionService;
  private final DeviceManager deviceManager;
  private final DataCollector dataCollector;
  private final IAnalysisService analysisService;

  public AcquisitionAPI(CollectionService collectionService, DeviceManager deviceManager, DataCollector dataCollector,
      IAnalysisService analysisService) {
    this.collectionService = collectionService;
    this.deviceManager = deviceManager;
    this.dataCollector = dataCollector;
    this.analysisService = analysisService;
  }

  /**
   * Zwraca listę dostępnych urządzeń w formacie czytelnym dla interfejsu
   * użytkownika.
   *
   * @return Lista łańcuchów znakowych opisujących urządzenia (ID, Nazwa, Typ).
   */
  public List<String> getAvailableDevices() {
    List<String> list = new ArrayList<>();
    for (Device d : deviceManager.getActiveDevices()) {
      list.add("[" + d.getId() + "] " + d.getName() + " (" + d.getMetricLabel() + ")");
    }
    return list;
  }

  /**
   * Wymusza natychmiastowy odczyt z pojedynczego czujnika.
   * <p>
   * Metoda pobiera odczyt fizyczny, zapisuje go w bazie danych oraz
   * przesyła do Modułu Analizy w czasie rzeczywistym.
   * </p>
   *
   * @param deviceId Identyfikator urządzenia (zgodny z bazą danych).
   * @return Wartość odczytu lub null, jeśli urządzenie nie istnieje lub wystąpił
   *         błąd.
   */
  public Double requestSensorRead(String deviceId) {
    Device device = deviceManager.getDeviceById(deviceId);
    if (device == null) {
      System.err.println("[API] Nie znaleziono urządzenia o ID: " + deviceId);
      return null;
    }
    try {
      Double value = device.readSensor();
      Double power = device.readCurrentPowerUsage();

      // Zapis do bazy danych
      Map<Device, Double> singleReadMap = new HashMap<>();
      singleReadMap.put(device, value);
      dataCollector.saveDataBatch(singleReadMap);

      // Wysyłka do Modułu Analizy (Push)
      if (analysisService != null) {
        analysisService.sendSensorUpdate(device.getId(), value, device.getMetricLabel(), power);
      }

      return value;
    } catch (Exception e) {
      System.err.println("[API] Błąd odczytu na żądanie: " + e.getMessage());
      return null;
    }
  }

  /**
   * Wymusza natychmiastowy cykl odczytu ze wszystkich zarejestrowanych urządzeń.
   * Może być wykorzystywane przez administratora do odświeżenia całego stanu
   * systemu.
   */
  public void requestAllData() {
    collectionService.forceRead();
  }

  /**
   * Rejestruje nowe urządzenie w systemie akwizycji.
   * <p>
   * Metoda posiada wbudowaną logikę "Smart Defaults" - automatycznie obsługuje
   * brakujące dane konfiguracyjne (null) oraz dostosowuje typ urządzenia
   * (np. wykrywa elementy sterowalne przy braku etykiety metryki).
   * </p>
   *
   * @param id          Unikalny identyfikator urządzenia (String).
   * @param name        Nazwa urządzenia (Producent + Model + Pokój).
   * @param min         Dolny zakres normy pomiarowej (może być null).
   * @param max         Górny zakres normy pomiarowej (może być null).
   * @param metricLabel Etykieta typu pomiaru (np. "temperatura_C").
   * @param powerUsage  Nominalne zużycie energii w Watach (może być null).
   */
  public void registerNewDevice(String id, String name, Number min, Number max, String metricLabel, Number powerUsage) {
    double safeMin = (min != null) ? min.doubleValue() : 0.0;
    double safeMax = (max != null) ? max.doubleValue() : 0.0;
    double safePower = (powerUsage != null) ? powerUsage.doubleValue() : 0.0;

    String safeId = (id != null) ? id : "UNKNOWN";
    String safeName = (name != null) ? name : "Unknown Device";

    String safeLabel;
    if (metricLabel == null || metricLabel.isEmpty()) {
      // Logika dla urządzeń bez zdefiniowanej metryki (np. oświetlenie)
      safeLabel = "jasnosc_procent";
      safeMin = 0.0;
      safeMax = 100.0;
    } else {
      safeLabel = metricLabel;
    }

    // Konfiguracja symulatora (Mock)
    double expectedValue = (safeMin + safeMax) / 2.0;
    if (safeMax == 0)
      expectedValue = 0;

    IDeviceConnector connector = new MockDeviceConnector(expectedValue, safePower);
    Device newDevice = new Device(safeId, safeName, safeMin, safeMax, safeLabel, connector);

    deviceManager.addNewDevice(newDevice);
  }
}
