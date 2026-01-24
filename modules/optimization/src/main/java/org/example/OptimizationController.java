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

        // boolean isBuildingOpen = isWithinWorkingHours();
        boolean isBuildingOpen = true;

        System.out.println("[Optymalizacja] Status budynku: " + (isBuildingOpen ? "OTWARTY" : "ZAMKNIĘTY") +
                " (Godziny: " + adminPref.getTimeOpen() + " - " + adminPref.getTimeClose() + ")");

        // 2. Pobierz listę pokoi
        List<Pokoj> rooms = controlService.getRoomsInBuilding(buildingId);

        if (rooms.isEmpty()) {
            System.out.println("[Optymalizacja] Brak pokoi w budynku.");
            return;
        }

        // 3. Iteruj po pokojach
        for (Pokoj room : rooms) {
            optimizeRoomBasedOnPreferences(room, isBuildingOpen);
        }

        System.out.println("[Optymalizacja] Zakończono optymalizację budynku.");
    }

    /**
     * Logika optymalizacji dla konkretnego pokoju.
     */
    private void optimizeRoomBasedOnPreferences(Pokoj room, boolean isBuildingOpen) {
        System.out.println("--- Optymalizacja Pokoju: " + room.getNumerPokoju() + " (ID: " + room.getId() + ") ---");

        List<Urzadzenie> devices = getDevicesFromRoom(room.getId());
        if (devices.isEmpty()) {
            System.out.println("   Brak urządzeń w tym pokoju.");
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

        // Obliczanie preferencji użytkowników
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
                System.out.println("   [CLIMATE] Urządzenie " + deviceId + ": Budynek zamknięty -> Tryb Eco (16°C).");
                controlDevice(device.getId(), "set_temp", 16.0);
            } else {
                if (currentVal < targetMinTemp) {
                    System.out.println(
                            "   [CLIMATE] Urządzenie " + deviceId + " (" + currentVal
                                    + "°C) -> Za zimno! Grzanie (Cel: " + targetMinTemp + ").");
                    controlDevice(device.getId(), "set_temp", targetMinTemp + 1.0);
                } else if (currentVal > targetMaxTemp) {
                    System.out.println("   [CLIMATE] Urządzenie " + deviceId + " (" + currentVal
                            + "°C) -> Za ciepło! Chłodzenie (Cel: " + targetMaxTemp + ").");
                    controlDevice(device.getId(), "set_temp", targetMaxTemp - 1.0);
                } else {
                    System.out.println("   [CLIMATE] Urządzenie " + deviceId + " -> Temperatura w normie.");
                }
            }
            return;
        }

        // Obsługa Oświetlenia (Teraz używa jasnosc_procent)
        if (isLightingDevice(device) && currentVal != null) {
            String params = device.getParametryPracy();
            // Sprawdź czy urządzenie w ogóle obsługuje tę metrykę (z init.sql)
            boolean supportsDimming = params != null && params.contains("jasnosc_procent");

            if (!isBuildingOpen && !userOverride) {
                System.out
                        .println("   [LIGHT] Urządzenie " + deviceId + ": Budynek zamknięty -> Wyłączam światło (0%).");
                // Ustawiamy 0% jasności
                controlDevice(device.getId(), "jasnosc_procent", 0.0);
            } else {
                double targetBrightness = 3.0; // Domyślnie 3/5

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
                        System.out.println("   [LIGHT] Średnie preferencje: Jasność=" + targetBrightness + " (1-5)");
                    }
                }

                // Konwersja 1-5 na 0-10 (tak jak chciał user: 11 stopni od 0 do 10)
                // 1 -> 2.0, 5 -> 10.0
                double targetScaled = Math.min(10.0, targetBrightness * 2.0);

                // Sprawdzenie czy aktualna wartość jest już poprawna (tolerancja 0.1)
                if (Math.abs(currentVal - targetScaled) < 0.1) {
                    System.out.println("   [LIGHT] Poziom jasności optymalny (" + currentVal + "). Brak akcji.");
                    return;
                }

                // Jeśli urządzenie jest ściemnialne (zgodnie z init.sql) lub ma metrykę
                // jasności
                if (supportsDimming) {
                    System.out.println("      -> Ustawianie jasności (0-10): " + targetScaled);
                    controlDevice(device.getId(), "jasnosc_procent", targetScaled);
                } else {
                    // Fallback dla on/off
                    double binaryState = (targetScaled >= 5.0) ? 10.0 : 0.0;

                    if (Math.abs(currentVal - binaryState) < 0.1) {
                        System.out.println("   [LIGHT] Poziom jasności optymalny (" + currentVal + "). Brak akcji.");
                        return;
                    }

                    System.out.println("      -> Przełączanie (On/Off): " + binaryState);
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
        // "set_temp" lub "docelowa_temperatura" - nastawy grzejników
        return params.contains("temperatura_C") ||
                params.contains("set_temp") ||
                params.contains("docelowa_temperatura");
    }

    private double calculateSolarProduction() {
        LocalTime now = LocalTime.now();
        int hour = now.getHour();
        // Simple simulation: Bell curve peaking at 12:00 (12 PM)
        // Production between 06:00 and 20:00
        if (hour < 6 || hour > 20) {
            return 0.0;
        }

        // Peak power: 5000 W (5kWp installation simulated)
        double maxPower = 5000.0;

        // Normalize time to -1.0 to 1.0 (where 0.0 is Noon)
        double timeFromNoon = (hour - 13.0) / 7.0; // 13:00 is center approx, range +/- 7h
        double productionFactor = Math.cos(timeFromNoon * Math.PI / 2.0);

        if (productionFactor < 0)
            productionFactor = 0;

        return maxPower * productionFactor;
    }

    private void checkEnergyLimit(Urzadzenie device) {
        LocalDateTime from = LocalDateTime.now().minusHours(24);
        LocalDateTime to = LocalDateTime.now();
        List<Odczyt> readings = forecastService.getReadingsForDevice(device.getId(), from, to);

        double avgUsage = calculateAverageFromReadings(readings);

        // Dynamic Limit Calculation
        double solarProduction = calculateSolarProduction();
        double baseLimit = adminPref.getMaxEnergyUsage();

        // Distribute solar production across active devices?
        // Or just treat it as a building-wide buffer.
        // Let's assume the 'maxEnergyUsage' is PER DEVICE.
        // So we add a share of solar production to this limit.
        List<Urzadzenie> activeDevices = getActiveDevices();
        double activeCount = Math.max(1.0, activeDevices.size());
        double perDeviceBonus = solarProduction / activeCount;

        double effectiveLimit = baseLimit + perDeviceBonus;

        if (solarProduction > 100) {
            System.out.println("   [OZE] Symulowana produkcja: " + String.format("%.2f", solarProduction)
                    + " W. Bonus na urządzenie: " + String.format("%.2f", perDeviceBonus) + " W");
        }

        if (avgUsage > effectiveLimit) {
            System.out.println("   [ENERGIA] Urządzenie " + device.getId() + " przekracza limit (" + avgUsage + " > "
                    + String.format("%.2f", effectiveLimit) + "). Ograniczam.");
            controlDevice(device.getId(), "limit_power", 50.0);
        } else {
            // System.out.println(" [ENERGIA] Urządzenie " + device.getId() + " w normie ("
            // + avgUsage + " < " + String.format("%.2f", effectiveLimit) + ")");
        }
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

        // SYNCHRONIZACJA Z SYMULACJĄ (Stopniowa zmiana w kontrolerze)
        if (acquisitionAPI != null) {
            // 1. Odczytaj aktualną wartość (symulowaną)
            Double currentVal = acquisitionAPI.requestSensorRead(String.valueOf(deviceId));

            double nextValue = setting; // Domyślnie skok (dla innych typów)

            // 2. Jeśli mamy odczyt i sterujemy temperaturą, rób to stopniowo
            if (currentVal != null && (attribute.equals("set_temp") || attribute.equals("temperatura_C"))) {
                double diff = setting - currentVal;
                double maxStep = 0.5; // Maksymalna zmiana na jeden cykl

                if (Math.abs(diff) > maxStep) {
                    nextValue = currentVal + Math.signum(diff) * maxStep;
                    System.out.println(
                            "      [GRADUAL] Zmiana z " + currentVal + " na " + nextValue + " (Cel: " + setting + ")");
                }
            }

            // 3. Wyślij obliczoną (pośrednią) wartość do symulacji
            acquisitionAPI.updateDeviceSimulation(String.valueOf(deviceId), nextValue);
        }

        if (acquisitionAPI != null)
            requestSensorReading(String.valueOf(deviceId));

        // Logowanie akcji usunięte na wniosek user.
        // logAction(deviceId, attribute, setting, "AUTO");
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