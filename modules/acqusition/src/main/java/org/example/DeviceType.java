package org.example;

public enum DeviceType {
    Temperature("temperature"),
    Humidity("humidity"),
    Pressure("pressure"),
    Power("power"),
    Luminosity("luminosity"),
    CO2Level("co2_level"),
    NoiseLevel("noise_level");

    private final String value;

    DeviceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DeviceType fromString(String type) {
        if (type == null) {
            return null;
        }
        for (DeviceType ct : DeviceType.values()) {
            if (ct.value.equalsIgnoreCase(type) || ct.name().equalsIgnoreCase(type)) {
                return ct;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return value;
    }
}