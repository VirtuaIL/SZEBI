package org.example;

public class Device {
    private String id;
    private String name;
    private double minRange;
    private double maxRange;
    private String metricLabel;
    private IDeviceConnector connector;

    public Device(String id, String name, double minRange, double maxRange, String metricLabel, IDeviceConnector connector) {
        this.id = id;
        this.name = name;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.metricLabel = metricLabel;
        this.connector = connector;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getMetricLabel() {
        return metricLabel;
    }

    public Double readSensor() throws Exception {
        if (!connector.checkConnection()) {
            throw new Exception("Błąd połączenia z urządzeniem " + id);
        }

        double value = connector.readValue();
        validateData(value);
        return value;
    }

    public Double readCurrentPowerUsage() {
        return connector.getPowerUsage();
    }

    private void validateData(double value) throws Exception {
        if (value < minRange || value > maxRange) {
            throw new Exception("Błąd walidacji: Wartość " + value + " poza zakresem dla " + name);
        }
    }
}