package org.example;

import java.time.LocalDateTime;

public enum AlertEventType {
  TemperatureExceedsThreshold("Temperature exceeds threshold", LocalDateTime.now()),
  HumidityExceedsThreshold("Humidity exceeds threshold", LocalDateTime.now()),
  PressureExceedsThreshold("Pressure exceeds threshold", LocalDateTime.now()),
  PowerExceedsThreshold("Power exceeds threshold", LocalDateTime.now()),
  LuminosityExceedsThreshold("Luminosity exceeds threshold", LocalDateTime.now()),
  CO2LevelExceedsThreshold("CO2 level exceeds threshold", LocalDateTime.now()),
  NoiseLevelExceedsThreshold("Noise level exceeds threshold", LocalDateTime.now());

  private final String message;
  private final LocalDateTime date;

  AlertEventType(String message, LocalDateTime date) {
    this.message = message;
    this.date = date;
  }

  public LocalDateTime getDate() {
    return this.date;
  }

  public String getMessage() {
    return this.message;
  }
}
