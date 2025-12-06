package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.DTO.*;
import org.example.interfaces.*;
import org.example.AdministratorPreferences;
import org.example.UserPreferences;
import org.example.OZEResource;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OptimizationController {
    private AdministratorPreferences adminPref;
    private boolean overrideAutomatization;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private IAlertData alertService;
    private IAcquisitionData acquisitionService;
    private IForecastingData forecastService;
    private IControlData controlService; // Tu jest dostęp do pokoi
    private IAnalyticsData analyticsService;

    private AcquisitionAPI acquisitionAPI;

    public OptimizationController() {
        this.overrideAutomatization = false;
        this.adminPref = new AdministratorPreferences();
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
    public void optimizeBuildingByRooms(int buildingId) {
        if (overrideAutomatization) {
            System.out.println("[Optymalizacja] Automatyzacja wyłączona.");
            return;
        }

        if (controlService == null) {
            System.err.println("[Optymalizacja] Brak serwisu kontroli (nie można pobrać pokoi).");
            return;
        }

        System.out.println("[Optymalizacja] Rozpoczynam optymalizację budynku ID: " + buildingId);

        // 1. Sprawdź czy budynek jest otwarty wg preferencji admina
//      boolean isBuildingOpen = isWithinWorkingHours();
        boolean isBuildingOpen = true; // Tymczasowo zawsze otwarty dla testów
        System.out.println("[Optymalizacja] Status budynku: " + (isBuildingOpen ? "OTWARTY" : "ZAMKNIĘTY") +
                " (Godziny: " + adminPref.getTimeOpen() + " - " + adminPref.getTimeClose() + ")");

        // 2. Pobierz listę pokoi
        List<Pokoj> rooms = controlService.getRoomsInBuilding(buildingId); // Zakładam, że ta metoda jest w interfejsie IControlData

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
            applyAdminRulesToDevice(device, isBuildingOpen);
        }
    }

    private void applyAdminRulesToDevice(Urzadzenie device, boolean isBuildingOpen) {
        String deviceId = String.valueOf(device.getId());
        Double currentVal = requestSensorReading(deviceId);

        // Obsługa Klimatu (Grzanie/Chłodzenie)
        if (doesDeviceMeasureTemperature(device) && currentVal != null) {
            double minTemp = adminPref.getPreferredMinTemp();
            double maxTemp = adminPref.getPreferredMaxTemp();

            if (!isBuildingOpen) {
                System.out.println("   [CLIMATE] Urządzenie " + deviceId + ": Budynek zamknięty -> Tryb Eco (16°C).");
                controlDevice(device.getId(), "set_temp", 16.0);
            }
            else {
                // Tryb dzienny
                if (currentVal < minTemp) {
                    System.out.println("   [CLIMATE] Urządzenie " + deviceId + " (" + currentVal + "°C) -> Za zimno! Grzanie.");
                    controlDevice(device.getId(), "set_temp", minTemp + 1.0);
                } else if (currentVal > maxTemp) {
                    System.out.println("   [CLIMATE] Urządzenie " + deviceId + " (" + currentVal + "°C) -> Za ciepło! Chłodzenie.");
                    controlDevice(device.getId(), "set_temp", maxTemp - 1.0);
                } else {
                    System.out.println("   [CLIMATE] Urządzenie " + deviceId + " -> Temperatura w normie.");
                }
            }
            return;
        }

        // Obsługa Oświetlenia
        if (isLightingDevice(device) && currentVal != null) {
            if (!isBuildingOpen) {
                System.out.println("   [LIGHT] Urządzenie " + deviceId + ": Budynek zamknięty -> Wyłączam światło.");
                controlDevice(device.getId(), "active", 0.0);
            }
            else {
                // Tu dodamy logikę: jeśli jest bardzo jasno (sensor > 80%), przygaś światło
                // Ale na razie tylko logujemy
                System.out.println("   [LIGHT] Urządzenie " + deviceId + " (Jasność: " + currentVal + "%) -> OK.");
            }
            return;
        }

        checkEnergyLimit(device);
    }

    // === Metody pomocnicze do logiki biznesowej ===

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
        if (params == null) return false;

        // Szukamy konkretnych kluczy wskazujących na temperaturę powietrza
        // "temperatura_C" - zazwyczaj odczyt z czujnika
        // "set_temp" lub "docelowa_temperatura" - nastawy grzejników
        return params.contains("temperatura_C") ||
                params.contains("set_temp") ||
                params.contains("docelowa_temperatura");
    }

    private void checkEnergyLimit(Urzadzenie device) {
        // Pobieramy historię z ostatnich 24h
        LocalDateTime from = LocalDateTime.now().minusHours(24);
        LocalDateTime to = LocalDateTime.now();
        List<Odczyt> readings = forecastService.getReadingsForDevice(device.getId(), from, to);

        double avgUsage = calculateAverageFromReadings(readings);

        if (avgUsage > adminPref.getMaxEnergyUsage()) {
            System.out.println("   [ENERGIA] Urządzenie " + device.getId() + " przekracza limit (" + avgUsage + " > " + adminPref.getMaxEnergyUsage() + "). Ograniczam.");
            controlDevice(device.getId(), "limit_power", 50.0); // Przykładowa akcja
        }
    }

    private List<Urzadzenie> getActiveDevices() {
        if (acquisitionService != null) return acquisitionService.getActiveDevices();
        System.err.println("[Optymalizacja] Brak połączenia z serwisem akwizycji!");
        return new ArrayList<>();
    }

    private List<Urzadzenie> getDevicesFromRoom(int roomId) {
        if (controlService != null) return controlService.getDevicesInRoom(roomId);
        System.err.println("[Optymalizacja] Brak połączenia z serwisem kontroli!");
        return new ArrayList<>();
    }

    public Double requestSensorReading(String deviceId) {
        if (acquisitionAPI == null) return null;
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
        if (controlService != null) controlService.updateDevice(device);

        // Symulacja odświeżenia
        if (acquisitionAPI != null) requestSensorReading(String.valueOf(deviceId));
    }

    // Twoja poprawiona metoda liczenia średniej (z parsowaniem JSON)
    private double calculateAverageFromReadings(List<Odczyt> readings) {
        if (readings == null || readings.isEmpty()) return 0.0;
        double sum = 0.0;
        int count = 0;
        for (Odczyt reading : readings) {
            String jsonPomiary = reading.getPomiary();
            if (jsonPomiary == null || jsonPomiary.isEmpty()) continue;
            try {
                JsonNode rootNode = objectMapper.readTree(jsonPomiary);
                double value = 0.0;
                boolean found = false;
                if (rootNode.has("zuzycie_kwh")) { value = rootNode.get("zuzycie_kwh").asDouble(); found = true; }
                else if (rootNode.has("wartosc")) { value = rootNode.get("wartosc").asDouble(); found = true; }
                else if (rootNode.has("value")) { value = rootNode.get("value").asDouble(); found = true; }

                if (found) { sum += value; count++; }
            } catch (Exception e) { /* ignore */ }
        }
        return count == 0 ? 0.0 : sum / count;
    }

    // === METODY CHECKERÓW, ALERTÓW I SETTERY (Standardowe) ===

    public void checkForAnomalies() {
        List<Urzadzenie> devices = getActiveDevices();
        if (devices.isEmpty()) return;

        LocalDateTime from = LocalDateTime.now().minusHours(1);
        LocalDateTime to = LocalDateTime.now();

        for (Urzadzenie device : devices) {
            List<Odczyt> readings = forecastService.getReadingsForDevice(device.getId(), from, to);
            if (readings != null) {
                for(Odczyt r : readings) {
                    if(isAnomalousReading(r)) System.out.println("ANOMALIA: " + device.getId());
                }
            }
        }
    }

    private boolean isAnomalousReading(Odczyt reading) {
        try {
            JsonNode root = objectMapper.readTree(reading.getPomiary());
            double val = root.path("wartosc").asDouble(0);
            return val < 0 || val > 10000;
        } catch (Exception e) { return false; }
    }

    public void setAlertService(IAlertData s) { this.alertService = s; }
    public void setAcquisitionService(IAcquisitionData s) { this.acquisitionService = s; }
    public void setForecastService(IForecastingData s) { this.forecastService = s; }
    public void setControlService(IControlData s) { this.controlService = s; }
    public void setAnalyticsService(IAnalyticsData s) { this.analyticsService = s; }
    public void setAdminPreferences(AdministratorPreferences p) { this.adminPref = p; }
}