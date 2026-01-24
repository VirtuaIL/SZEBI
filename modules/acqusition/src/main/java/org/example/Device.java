package org.example;

public class Device {
    private String id;
    private String name;
    private double minRange;
    private double maxRange;
    private DeviceType metricLabel;
    private IDeviceConnector connector;

    public Device(String id, String name, double minRange, double maxRange, DeviceType metricLabel,
                  IDeviceConnector connector) {
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

    public DeviceType getMetricLabel() {
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

    public void simulateStateChange(double value) {
        connector.setValue(value);
    } // TODO: Simulate value changes
}