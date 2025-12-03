package org.example;

public class Device {
    private String id;
    private String name;
    private double minRange;
    private double maxRange;
    private IDeviceConnector connector;

    // Etykieta pomiaru (np. "temperatura_C", "wilgotnosc_procent")
    private String metricLabel;

    // Aktualizacja konstruktora
    public Device(String id, String name, double minRange, double maxRange, String metricLabel, IDeviceConnector connector) {
        this.id = id;
        this.name = name;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.metricLabel = metricLabel; // Zapamiętujemy etykietę
        this.connector = connector;
    }

    public String getId() { return id; }

    // Getter do etykiety
    public String getMetricLabel() { return metricLabel; }

    public Double readSensor() throws Exception {
        if (!connector.checkConnection()) {
            throw new Exception("Błąd połączenia z urządzeniem " + id);
        }
        double value = connector.readValue();
        validateData(value);
        return value;
    }

    private void validateData(double value) throws Exception {
        if (value < minRange || value > maxRange) {
            throw new Exception("Błąd walidacji: Wartość " + value + " poza zakresem dla " + name);
        }
    }

    public Double readCurrentPowerUsage() {
        return connector.getPowerUsage();
    }
}