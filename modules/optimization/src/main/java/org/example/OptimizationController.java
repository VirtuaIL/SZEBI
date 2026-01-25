package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.DTO.*;
import org.example.interfaces.*;
import org.example.alerts.AlertService;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OptimizationController {
    private AdministratorPreferencesDTO adminPref;
    private boolean overrideAutomatization;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private AlertService alertService;
    private IAcquisitionData acquisitionService;
    private IForecastingData forecastService;
    private IControlData controlService;
    private IAnalyticsData analyticsService;

    private AcquisitionAPI acquisitionAPI;

    public OptimizationController() {
        this.overrideAutomatization = false;
        this.adminPref = new AdministratorPreferencesDTO();
    }

    public boolean isOverrideAutomatization() {
        return overrideAutomatization;
    }

    public void setOverrideAutomatization(boolean overrideAutomatization) {
        this.overrideAutomatization = overrideAutomatization;
    }

    public AdministratorPreferencesDTO getAdminPref() {
        return adminPref;
    }

    public void setAcquisitionAPI(AcquisitionAPI acquisitionAPI) {
        this.acquisitionAPI = acquisitionAPI;
    }

    /**
     * Główna funkcja uruchamiająca optymalizację per pokój.
     * 1. Pobiera listę pokoi w budynku.
     * 2. Sprawdza godziny otwarcia z preferencji Admina.
     * 3. Dostosowuje urządzenia w każdym pokoju.
     */
    public void optimizeEnergyConsumption(int buildingId) {
        if (overrideAutomatization) {
            System.out.println("[Optymalizacja] Automatyzacja wyłączona.");
            return;
        }

        if (controlService == null) {
            System.err.println("[Optymalizacja] Brak serwisu kontroli (nie można pobrać pokoi).");
            return;
        }

        System.out.println("[Optymalizacja] Rozpoczynam optymalizację budynku ID: " + buildingId);

        // Tymczasowo zawsze otwarty dla testów
        if (userService != null) {
            loadAdminPreferences();
        }

        // Odkomentuj poniżej, aby używać godzin pracy
        // boolean isBuildingOpen = isWithinWorkingHours();
        boolean isBuildingOpen = true;

        System.out.println("[Optymalizacja] Status budynku: " + (isBuildingOpen ? "OTWARTY" : "ZAMKNIĘTY") +
                " (Godziny: " + adminPref.getTimeOpen() + " - " + adminPref.getTimeClose() + ")");

        // Pobierz listę pokoi
        List<Pokoj> rooms = controlService.getRoomsInBuilding(buildingId);

        if (rooms.isEmpty()) {
            System.out.println("[Optymalizacja] Brak pokoi w budynku.");
            return;
        }

        // Oblicz zużycie energii dla urządzeń w budynku
        if (acquisitionAPI != null) {
            double buildingPower = calculateBuildingPowerUsage(rooms);
            System.out.println(String.format("[ENERGIA] Aktualne zużycie budynku: %.2f W", buildingPower));
        }

        // Iteruj po pokojach
        for (Pokoj room : rooms) {
            optimizeRoomBasedOnPreferences(room, isBuildingOpen);
        }

        // Wyświetl zużycie energii po optymalizacji
        if (acquisitionAPI != null) {
            double buildingPowerAfter = calculateBuildingPowerUsage(rooms);
            System.out.println(String.format("[ENERGIA] Zużycie po optymalizacji: %.2f W", buildingPowerAfter));
        }

        System.out.println("[Optymalizacja] Zakończono optymalizację budynku.");
    }

    /**
     * Logika optymalizacji dla konkretnego pokoju.
     */
    private void optimizeRoomBasedOnPreferences(Pokoj room, boolean isBuildingOpen) {
        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.println(String.format("║ %-50s ║",
                "Optymalizacja Pokoju: " + room.getNumerPokoju() + " (ID: " + room.getId() + ")"));
        System.out.println("╚════════════════════════════════════════════════════╝");

        List<Urzadzenie> devices = getDevicesFromRoom(room.getId());
        if (devices.isEmpty()) {
            System.out.println("   [INFO] Brak urządzeń w tym pokoju.");
            return;
        }

        for (Urzadzenie device : devices) {
            applyAdminRulesToDevice(device, isBuildingOpen, room);
        }
    }

    private IUserData userService;

    public void setUserService(IUserData userService) {
        this.userService = userService;
    }

    private void applyAdminRulesToDevice(Urzadzenie device, boolean isBuildingOpen, Pokoj room) {
        String deviceId = String.valueOf(device.getId());
        Double currentVal = requestSensorReading(deviceId);

        double targetMinTemp = adminPref.getPreferredMinTemp();
        double targetMaxTemp = adminPref.getPreferredMaxTemp();
        boolean userOverride = false;

        List<Integer> userIds = room.getUzytkownicyIds();
        if (userIds != null && !userIds.isEmpty() && userService != null) {
            double sumComfort = 0;
            int count = 0;

            for (Integer uid : userIds) {
                Uzytkownik u = userService.getUserById(uid);
                if (u != null && u.getPreferencje() != null) {
                    sumComfort += u.getPreferencje().getComfortLevel();
                    count++;
                }
            }

            if (count > 0) {
                double avgComfort = sumComfort / count;
                System.out.println("   [PREFERENCJE] Wyliczono średni poziom komfortu: " + avgComfort + " (skala 1-5)");

                // Mapowanie: scale 1-5 to Admin Range
                double range = targetMaxTemp - targetMinTemp;
                double normalized = (avgComfort - 1.0) / 4.0;
                double calculatedTemp = targetMinTemp + (normalized * range);

                System.out.println("      -> Zmapowano na temperaturę: " + calculatedTemp + "°C");

                targetMinTemp = calculatedTemp - 0.5;
                targetMaxTemp = calculatedTemp + 0.5;
                userOverride = true;
            }
        }

        // Obsługa Klimatu (Grzanie/Chłodzenie)
        if (doesDeviceMeasureTemperature(device) && currentVal != null) {

            if (!isBuildingOpen && !userOverride) {
                System.out.println(String.format("   %-10s [ID:%s] | Budynek ZAMKNIĘTY -> Tryb Eco (16°C)", "[CLIMATE]",
                        deviceId));
                controlDevice(device.getId(), "set_temp", 16.0);
            } else {
                if (currentVal < targetMinTemp) {
                    System.out.println(String.format("   %-10s [ID:%s] | %.2f°C -> Za zimno! Grzanie (Cel: %.1f)",
                            "[CLIMATE]", deviceId, currentVal, targetMinTemp));
                    controlDevice(device.getId(), "set_temp", targetMinTemp + 1.0);
                } else if (currentVal > targetMaxTemp) {
                    System.out.println(String.format("   %-10s [ID:%s] | %.2f°C -> Za ciepło! Chłodzenie (Cel: %.1f)",
                            "[CLIMATE]", deviceId, currentVal, targetMaxTemp));
                    controlDevice(device.getId(), "set_temp", targetMaxTemp - 1.0);
                } else {
                    System.out.println(String.format("   %-10s [ID:%s] | %.2f°C -> Temperatura OK", "[CLIMATE]",
                            deviceId, currentVal));
                }
            }
            return;
        }

        // Obsługa Oświetlenia
        if (isLightingDevice(device) && currentVal != null) {
            String params = device.getParametryPracy();
            // Sprawdź czy urządzenie w ogóle obsługuje tę metrykę
            boolean supportsDimming = params != null && params.contains("jasnosc_procent");

            if (!isBuildingOpen && !userOverride) {
                System.out.println(String.format("   %-10s [ID:%s] | Budynek ZAMKNIĘTY -> Wyłączam światło (0%%)",
                        "[LIGHT]", deviceId));
                // Ustawiamy 0% jasności
                controlDevice(device.getId(), "jasnosc_procent", 0.0);
            } else {
                double targetBrightness = 3.0;

                List<Integer> roomUserIds = room.getUzytkownicyIds();
                if (roomUserIds != null && !roomUserIds.isEmpty() && userService != null) {
                    double sumBright = 0;
                    int count = 0;
                    for (Integer uid : roomUserIds) {
                        Uzytkownik u = userService.getUserById(uid);
                        if (u != null && u.getPreferencje() != null) {
                            sumBright += u.getPreferencje().getPreferredLighting(); // 1-5
                            count++;
                        }
                    }
                    if (count > 0) {
                        targetBrightness = sumBright / count;
                        System.out.println(String.format("   %-10s [ID:%s] | Średnie preferencje: %.2f (1-5)",
                                "[LIGHT]", deviceId, targetBrightness));
                    }
                }

                // Konwersja 1-5 na 0-100
                double targetScaled = Math.min(100.0, targetBrightness * 20.0);

                // Sprawdzenie, czy aktualna wartość jest już poprawna
                // (tolerancja 1.0 dla szumu +/- 0.5)
                if (Math.abs(currentVal - targetScaled) < 1.0) {
                    System.out.println(String.format("   %-10s [ID:%s] | Jasność OK (%.1f%%). Brak akcji.", "[LIGHT]",
                            deviceId, currentVal));
                    return;
                }

                // Jeśli urządzenie jest ściemnialne (zgodnie z init.sql) lub ma metrykę
                // jasności
                if (supportsDimming) {
                    System.out.println(String.format("      -> Ustawiam jasność: %.1f%%", targetScaled));
                    controlDevice(device.getId(), "jasnosc_procent", targetScaled);
                } else {
                    // Fallback dla on/off
                    double binaryState = (targetScaled >= 50.0) ? 100.0 : 0.0;

                    if (Math.abs(currentVal - binaryState) < 1.0) {
                        System.out.println(String.format("   %-10s [ID:%s] | Stan OK (%.1f%%). Brak akcji.", "[LIGHT]",
                                deviceId, currentVal));
                        return;
                    }

                    System.out.println(String.format("      -> Przełączam (On/Off): %.0f%%", binaryState));
                    controlDevice(device.getId(), "jasnosc_procent", binaryState);
                }
            }
            return;
        }

        checkEnergyLimit(device);
    }

    private boolean isWithinWorkingHours() {
        LocalTime now = LocalTime.now();
        LocalTime open = LocalTime.parse(adminPref.getTimeOpen(), DateTimeFormatter.ISO_LOCAL_TIME); // np. "08:00"
        LocalTime close = LocalTime.parse(adminPref.getTimeClose(), DateTimeFormatter.ISO_LOCAL_TIME); // np. "20:00"

        return now.isAfter(open) && now.isBefore(close);
    }

    private boolean isLightingDevice(Urzadzenie device) {
        String params = device.getParametryPracy();
        return params != null && (params.contains("jasnosc") || params.contains("sciemnialna"));
    }

    private boolean doesDeviceMeasureTemperature(Urzadzenie device) {
        String params = device.getParametryPracy();
        if (params == null)
            return false;

        // "temperatura_C" - zazwyczaj odczyt z czujnika
        return params.contains("temperatura_C");
    }

    public double calculateSolarProduction() {
        LocalTime now = LocalTime.now();
        int hour = now.getHour();
        if (hour < 6 || hour > 20) {
            return 0.0;
        }

        double maxPower = 10.0;

        double timeFromNoon = (hour - 13.0) / 7.0;
        double productionFactor = Math.cos(timeFromNoon * Math.PI / 2.0);

        if (productionFactor < 0)
            productionFactor = 0;

        return maxPower * productionFactor;
    }

    private void checkEnergyLimit(Urzadzenie device) {
        if (forecastServiceAPI == null) {
            System.err.println("[Optymalizacja] Brak ForecastServiceAPI - pomijam sprawdzanie limitów.");
            return;
        }

        // Sprawdzamy prognozę na najbliższe 4 godziny
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusHours(4);

        List<Prognoza> forecasts = forecastServiceAPI.getForecasts(device.getId(), now, future);

        double predictedAvgUsage = calculateAverageFromForecasts(forecasts);

        if (forecasts.isEmpty()) {
            System.out.println(
                    "   [ENERGIA] Brak prognoz dla urządzenia " + device.getId() + ". Zakładam typowe zużycie.");
            return;
        }

        double solarProduction = calculateSolarProduction();
        double baseLimit = adminPref.getMaxEnergyUsage();

        List<Urzadzenie> activeDevices = getActiveDevices();
        double activeCount = Math.max(1.0, activeDevices.size());
        double perDeviceBonus = solarProduction / activeCount;

        double effectiveLimit = baseLimit + perDeviceBonus;

        if (solarProduction > 100) {
            System.out.println("   [OZE] Symulowana produkcja: " + String.format("%.2f", solarProduction)
                    + " W. Bonus na urządzenie: " + String.format("%.2f", perDeviceBonus) + " W");
        }

        System.out
                .println("   [ENERGIA] Prognozowane średnie zużycie (4h): " + String.format("%.2f", predictedAvgUsage));

        if (predictedAvgUsage > effectiveLimit) {
            System.out.println(
                    "   [ENERGIA] Urządzenie " + device.getId() + " PRZEKROCZY limit (" + predictedAvgUsage + " > "
                            + String.format("%.2f", effectiveLimit) + "). Ograniczam prewencyjnie.");
            applyEnergySavingAction(device);
        } else {
            System.out.println(" [ENERGIA] Urządzenie " + device.getId() + " w normie prognozy ("
                    + predictedAvgUsage + " < " + String.format("%.2f", effectiveLimit) + ")");
        }
    }

    /**
     * Stosuje akcję oszczędzania energii dla urządzenia.
     * - Dla urządzeń klimatycznych: obniża temperaturę (ale nie poniżej minimum
     * admina)
     * - Dla oświetlenia: zmniejsza jasność lub wyłącza
     */
    private void applyEnergySavingAction(Urzadzenie device) {
        String deviceId = String.valueOf(device.getId());
        Double currentVal = requestSensorReading(deviceId);

        // Urządzenia klimatyczne (temperatura)
        if (doesDeviceMeasureTemperature(device)) {
            double minTemp = adminPref.getPreferredMinTemp();

            if (currentVal != null && currentVal > minTemp) {
                // Obniżamy o 1-2 stopnie, ale nie poniżej minimum
                double reducedTemp = Math.max(minTemp, currentVal - 2.0);
                System.out.println(String.format(
                        "   [OSZCZĘDNOŚĆ] Urządzenie %s: obniżam temperaturę z %.1f°C do %.1f°C (min: %.1f°C)",
                        deviceId, currentVal, reducedTemp, minTemp));
                controlDevice(device.getId(), "set_temp", reducedTemp);
            } else {
                System.out.println(String.format("   [OSZCZĘDNOŚĆ] Urządzenie %s: temperatura już na minimum (%.1f°C)",
                        deviceId, minTemp));
            }
            return;
        }

        // Oświetlenie
        if (isLightingDevice(device)) {
            if (currentVal != null && currentVal > 0) {
                // Zmniejszamy jasność o 30% lub wyłączamy jeśli już niska
                double reducedBrightness = currentVal * 0.7;
                if (reducedBrightness < 20.0) {
                    reducedBrightness = 0.0; // Wyłączamy całkowicie
                    System.out
                            .println(String.format("   [OSZCZĘDNOŚĆ] Urządzenie %s: wyłączam oświetlenie (było %.1f%%)",
                                    deviceId, currentVal));
                } else {
                    System.out.println(
                            String.format("   [OSZCZĘDNOŚĆ] Urządzenie %s: zmniejszam jasność z %.1f%% do %.1f%%",
                                    deviceId, currentVal, reducedBrightness));
                }
                controlDevice(device.getId(), "jasnosc_procent", reducedBrightness);
            } else {
                System.out
                        .println(String.format("   [OSZCZĘDNOŚĆ] Urządzenie %s: oświetlenie już wyłączone", deviceId));
            }
            return;
        }

        // Inne urządzenia - logujemy tylko
        System.out.println(String.format("   [OSZCZĘDNOŚĆ] Urządzenie %s: brak możliwości redukcji zużycia", deviceId));
    }

    private double calculateAverageFromForecasts(List<Prognoza> forecasts) {
        if (forecasts == null || forecasts.isEmpty())
            return 0.0;

        double sum = 0;
        for (Prognoza p : forecasts) {
            sum += p.getPrognozowanaWartosc();
        }
        return sum / forecasts.size();
    }

    private List<Urzadzenie> getActiveDevices() {
        if (acquisitionService != null)
            return acquisitionService.getActiveDevices();
        System.err.println("[Optymalizacja] Brak połączenia z serwisem akwizycji!");
        return new ArrayList<>();
    }

    private List<Urzadzenie> getDevicesFromRoom(int roomId) {
        if (controlService != null)
            return controlService.getDevicesInRoom(roomId);
        System.err.println("[Optymalizacja] Brak połączenia z serwisem kontroli!");
        return new ArrayList<>();
    }

    /**
     * Oblicza sumę zużycia energii dla wszystkich urządzeń w pokojach budynku.
     */
    private double calculateBuildingPowerUsage(List<Pokoj> rooms) {
        double totalPower = 0.0;
        for (Pokoj room : rooms) {
            List<Urzadzenie> devices = getDevicesFromRoom(room.getId());
            for (Urzadzenie device : devices) {
                totalPower += acquisitionAPI.getPowerUsageForDevice(String.valueOf(device.getId()));
            }
        }
        return totalPower;
    }

    public Double requestSensorReading(String deviceId) {
        if (acquisitionAPI == null)
            return null;
        return acquisitionAPI.requestSensorRead(deviceId);
    }

    public void controlDevice(int deviceId, String attribute, double setting) {
        List<Urzadzenie> devices = getActiveDevices();
        Urzadzenie device = devices.stream().filter(d -> d.getId() == deviceId).findFirst().orElse(null);

        if (device == null) {
            System.err.println("[Optymalizacja] Nie znaleziono urządzenia ID: " + deviceId);
            return;
        }

        System.out.println("      -> WYSYŁANIE KOMENDY: " + attribute + " = " + setting);

        if (acquisitionAPI != null) {
            Double currentVal = acquisitionAPI.requestSensorRead(String.valueOf(deviceId));

            double nextValue = setting;

            if (currentVal != null && (attribute.equals("set_temp") || attribute.equals("temperatura_C"))) {
                double diff = setting - currentVal;
                double maxStep = 0.5;

                if (Math.abs(diff) > maxStep) {
                    nextValue = currentVal + Math.signum(diff) * maxStep;
                    System.out.println(String.format("      -> Dążenie do celu: %.1f°C", setting));
                } else {
                    System.out.println(String.format("      -> Temperatura w normie (cel: %.1f°C)", setting));
                }
            }

            acquisitionAPI.updateDeviceSimulation(String.valueOf(deviceId), nextValue);
        }

        if (acquisitionAPI != null)
            requestSensorReading(String.valueOf(deviceId));

    }

    private double calculateAverageFromReadings(List<Odczyt> readings) {
        if (readings == null || readings.isEmpty())
            return 0.0;
        double sum = 0.0;
        int count = 0;
        for (Odczyt reading : readings) {
            String jsonPomiary = reading.getPomiary();
            if (jsonPomiary == null || jsonPomiary.isEmpty())
                continue;
            try {
                JsonNode rootNode = objectMapper.readTree(jsonPomiary);
                double value = 0.0;
                boolean found = false;
                if (rootNode.has("zuzycie_kwh")) {
                    value = rootNode.get("zuzycie_kwh").asDouble();
                    found = true;
                } else if (rootNode.has("wartosc")) {
                    value = rootNode.get("wartosc").asDouble();
                    found = true;
                } else if (rootNode.has("value")) {
                    value = rootNode.get("value").asDouble();
                    found = true;
                }

                if (found) {
                    sum += value;
                    count++;
                }
            } catch (Exception e) {
                /* ignore */ }
        }
        return count == 0 ? 0.0 : sum / count;
    }

    public void checkForAnomalies() {
        List<Urzadzenie> devices = getActiveDevices();
        if (devices.isEmpty())
            return;

        LocalDateTime from = LocalDateTime.now().minusHours(1);
        LocalDateTime to = LocalDateTime.now();

        for (Urzadzenie device : devices) {
            List<Odczyt> readings = forecastService.getReadingsForDevice(device.getId(), from, to);
            if (readings != null) {
                for (Odczyt r : readings) {
                    if (isAnomalousReading(r)) {
                        System.out.println("ANOMALIA: " + device.getId());

                        if (alertService != null) {
                            org.example.alerts.dto.ZgloszenieDTO zgloszenie = new org.example.alerts.dto.ZgloszenieDTO(
                                    "Wykryto anomalię w odczytach urządzenia (wartość poza zakresem).",
                                    device.getId(),
                                    org.example.DTO.Alert.AlertSeverity.WARNING,
                                    "OPTIMIZATION_MODULE");
                            alertService.zglosAnomalie(zgloszenie);
                            System.out.println(
                                    "   -> Zgłoszono anomalię przez AlertService dla urządzenia " + device.getId());
                        }
                    }
                }
            }
        }
    }

    private boolean isAnomalousReading(Odczyt reading) {
        try {
            JsonNode root = objectMapper.readTree(reading.getPomiary());
            double val = root.path("wartosc").asDouble(0);
            return val < 0 || val > 10000;
        } catch (Exception e) {
            return false;
        }
    }

    public void setAlertService(AlertService s) {
        this.alertService = s;
    }

    public void setAcquisitionService(IAcquisitionData s) {
        this.acquisitionService = s;
    }

    public void setForecastService(IForecastingData s) {
        this.forecastService = s;
    }

    public void setControlService(IControlData s) {
        this.controlService = s;
    }

    public void setAnalyticsService(IAnalyticsData s) {
        this.analyticsService = s;
    }

    private java.util.concurrent.ScheduledExecutorService scheduler;
    private java.util.concurrent.ScheduledFuture<?> scheduledTask;
    private IOptimizationData optimizationData;
    private ForecastServiceAPI forecastServiceAPI;

    public void setOptimizationData(IOptimizationData data) {
        this.optimizationData = data;
    }

    public void setForecastServiceAPI(ForecastServiceAPI api) {
        this.forecastServiceAPI = api;
    }

    public void startAutoCycle(int buildingId, int intervalSeconds) {
        if (scheduler == null) {
            scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1);
        }
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            System.out.println("[Optymalizacja] Cykl już jest uruchomiony.");
            return;
        }

        Runnable task = () -> {
            try {
                optimizeEnergyConsumption(buildingId);
                checkForAnomalies();
            } catch (Exception e) {
                System.err.println("[Optymalizacja] Błąd w cyklu: " + e.getMessage());
                e.printStackTrace();
            }
        };

        scheduledTask = scheduler.scheduleAtFixedRate(task, 0, intervalSeconds, java.util.concurrent.TimeUnit.SECONDS);
        System.out.println("[Optymalizacja] Uruchomiono automatyczny cykl co " + intervalSeconds + " sekund.");
    }

    public void stopAutoCycle() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            System.out.println("[Optymalizacja] Zatrzymano automatyczny cykl.");
        }
    }

    public void setAdminPreferences(AdministratorPreferencesDTO p) {
        this.adminPref = p;
    }

    private void loadAdminPreferences() {
        try {
            List<Uzytkownik> admins = userService.getUsersByRole(1); // 1 = Administrator
            if (!admins.isEmpty()) {
                Uzytkownik admin = admins.get(0);
                String rawJson = admin.getRawPreferences();
                if (rawJson != null && !rawJson.isEmpty()) {
                    // Deserializacja do DTO
                    this.adminPref = objectMapper.readValue(rawJson, AdministratorPreferencesDTO.class);
                    System.out
                            .println("[Optymalizacja] Załadowano preferencje administratora z DB ID: " + admin.getId());
                }
            } else {
                System.out.println("[Optymalizacja] Nie znaleziono administratora w DB. Używam domyślnych.");
            }
        } catch (Exception e) {
            System.err.println("[Optymalizacja] Błąd ładowania preferencji admina: " + e.getMessage());
        }
    }

}