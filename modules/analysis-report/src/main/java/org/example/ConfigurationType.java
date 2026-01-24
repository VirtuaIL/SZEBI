package org.example;

public enum ConfigurationType {
  Temperature(DeviceType.Temperature),
  Humidity(DeviceType.Humidity),
  Pressure(DeviceType.Pressure),
  Power(DeviceType.Power),
  Luminosity(DeviceType.Luminosity),
  CO2Level(DeviceType.CO2Level),
  NoiseLevel(DeviceType.NoiseLevel);

  private final DeviceType deviceType;

  ConfigurationType(DeviceType deviceType) {
    this.deviceType = deviceType;
  }

  public DeviceType getDeviceType() {
    return deviceType;
  }

  public String getValue() {
    return deviceType.getValue();
  }

  public static ConfigurationType fromString(String type) {
    if (type == null) {
      return null;
    }
    for (ConfigurationType ct : ConfigurationType.values()) {
      if (ct.getValue().equalsIgnoreCase(type) || ct.name().equalsIgnoreCase(type)) {
        return ct;
      }
    }
    return null;
  }

  public static ConfigurationType fromDeviceType(DeviceType deviceType) {
    if (deviceType == null) {
      return null;
    }
    for (ConfigurationType ct : ConfigurationType.values()) {
      if (ct.deviceType == deviceType) {
        return ct;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return deviceType.toString();
  }
}
