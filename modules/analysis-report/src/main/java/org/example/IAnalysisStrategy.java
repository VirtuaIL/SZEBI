package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

interface IAnalysisStrategy {
  List<AlertEvent> analyze(Map<ConfigurationType, List<Double>> data);

  default List<AlertEvent> analyzeWithDeviceId(List<SensorReading> readings) {
    // Domyślna implementacja - wywołuje starą metodę bez deviceId
    Map<ConfigurationType, List<Double>> data = new HashMap<>();
    for (SensorReading reading : readings) {
      data.computeIfAbsent(reading.getConfigurationType(), k -> new ArrayList<>())
          .add(reading.getValue());
    }
    return analyze(data);
  }

  class DefaultStrategy implements IAnalysisStrategy {
    @Override
    public List<AlertEvent> analyze(Map<ConfigurationType, List<Double>> data) {
      List<AlertEvent> alerts = new ArrayList<>();

      if (data == null || data.isEmpty()) {
        return alerts;
      }

      for (Map.Entry<ConfigurationType, List<Double>> entry : data.entrySet()) {
        ConfigurationType type = entry.getKey();
        List<Double> values = entry.getValue();

        for (Double value : values) {
          if (isOutOfTypicalRange(type, value)) {
            AlertEventType alertType = getAlertForConfigurationType(type);
            if (alertType != null) {
              AlertEvent event = new AlertEvent(alertType);
              if (!alerts.contains(event)) {
                alerts.add(event);
              }
            }
          }
        }
      }

      return alerts;
    }

    @Override
    public List<AlertEvent> analyzeWithDeviceId(List<SensorReading> readings) {
      List<AlertEvent> alerts = new ArrayList<>();

      if (readings == null || readings.isEmpty()) {
        return alerts;
      }

      for (SensorReading reading : readings) {
        ConfigurationType type = reading.getConfigurationType();
        double value = reading.getValue();
        String deviceId = reading.getDeviceId();

        if (isOutOfTypicalRange(type, value)) {
          AlertEventType alertType = getAlertForConfigurationType(type);
          if (alertType != null) {
            AlertEvent event = new AlertEvent(alertType, deviceId);
            // Dodaj alert tylko jeśli nie ma jeszcze takiego samego (typ + deviceId)
            if (!alerts.contains(event)) {
              alerts.add(event);
            }
          }
        }
      }

      return alerts;
    }

    private boolean isOutOfTypicalRange(ConfigurationType type, double value) {
      Map<String, Double> ranges = getTypicalRanges(type);
      if (ranges.isEmpty()) {
        return false;
      }
      double min = ranges.getOrDefault("min_typical", Double.MIN_VALUE);
      double max = ranges.getOrDefault("max_typical", Double.MAX_VALUE);
      return value < min || value > max;
    }

    private Map<String, Double> getTypicalRanges(ConfigurationType type) {
      Map<String, Double> ranges = new HashMap<>();
      if (type == null) {
        return ranges;
      }

      switch (type) {
        case Temperature:
          ranges.put("min_typical", 15.0);
          ranges.put("max_typical", 30.0);
          break;
        case Humidity:
          ranges.put("min_typical", 30.0);
          ranges.put("max_typical", 70.0);
          break;
        case Pressure:
          ranges.put("min_typical", 980.0);
          ranges.put("max_typical", 1030.0);
          break;
        case Power:
          ranges.put("min_typical", 0.0);
          ranges.put("max_typical", 3000.0);
          break;
        case Luminosity:
          ranges.put("min_typical", 0.0);
          ranges.put("max_typical", 100.0);
          break;
        case CO2Level:
          ranges.put("min_typical", 400.0);
          ranges.put("max_typical", 2000.0);
          break;
        case NoiseLevel:
          ranges.put("min_typical", 30.0);
          ranges.put("max_typical", 90.0);
          break;
        default:
          break;
      }
      return ranges;
    }

    private AlertEventType getAlertForConfigurationType(ConfigurationType type) {
      if (type == null) {
        return null;
      }
      switch (type) {
        case Temperature:
          return AlertEventType.TemperatureExceedsThreshold;
        case Humidity:
          return AlertEventType.HumidityExceedsThreshold;
        case Pressure:
          return AlertEventType.PressureExceedsThreshold;
        case Power:
          return AlertEventType.PowerExceedsThreshold;
        case Luminosity:
          return AlertEventType.LuminosityExceedsThreshold;
        case CO2Level:
          return AlertEventType.CO2LevelExceedsThreshold;
        case NoiseLevel:
          return AlertEventType.NoiseLevelExceedsThreshold;
        default:
          return null;
      }
    }
  }
}
