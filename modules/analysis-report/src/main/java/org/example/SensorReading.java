package org.example;

class SensorReading {
    private final String deviceId;
    private final double value;
    private final ConfigurationType configurationType;

    public SensorReading(String deviceId, double value, ConfigurationType configurationType) {
        this.deviceId = deviceId;
        this.value = value;
        this.configurationType = configurationType;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public double getValue() {
        return value;
    }

    public ConfigurationType getConfigurationType() {
        return configurationType;
    }

    @Override
    public String toString() {
        return "SensorReading{" +
                "deviceId='" + deviceId + '\'' +
                ", value=" + value +
                ", configurationType=" + configurationType +
                '}';
    }
}
