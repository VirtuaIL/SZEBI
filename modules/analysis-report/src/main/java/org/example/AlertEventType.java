package org.example;

public enum AlertEventType {
  TemperatureExceedsThreshold("Temperature exceeds threshold"),
  HumidityExceedsThreshold("Humidity exceeds threshold"),
  PressureExceedsThreshold("Pressure exceeds threshold"),
  PowerExceedsThreshold("Power exceeds threshold"),
  LuminosityExceedsThreshold("Luminosity exceeds threshold"),
  CO2LevelExceedsThreshold("CO2 level exceeds threshold"),
  NoiseLevelExceedsThreshold("Noise level exceeds threshold");

  private final String message;

  AlertEventType(String message) {
    this.message = message;
  }

  public String getMessage() {
    return this.message;
  }
}
