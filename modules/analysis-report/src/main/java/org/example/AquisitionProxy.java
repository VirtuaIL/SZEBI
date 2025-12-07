package org.example;

import java.util.*;
import java.util.stream.Collectors;

public class AquisitionProxy implements IAnalysisService {
  public static AquisitionProxy singleton = new AquisitionProxy();

  List<InternalSensors> sensors = new ArrayList<>();

  private class InternalSensors {
    private final String id;
    private final double value;
    private final String label;
    private final double powerUsage;

    InternalSensors(String id, double value, String label, double powerUsage) {
      this.id = id;
      this.value = value;
      this.label = label;
      this.powerUsage = powerUsage;
    }

    String getLabel() {
      return label;
    }

    String getId() {
      return id;
    }

    double getValue() {
      return value;
    }

    double getPowerUsage() {
      return powerUsage;
    }
  }

  public Map<String, List<Double>> getLabelValues() {
    return sensors.stream()
        .collect(Collectors.groupingBy(
            InternalSensors::getLabel,
            Collectors.mapping(
                InternalSensors::getValue,
                Collectors.toUnmodifiableList())))
        .entrySet().stream()
        .collect(Collectors.toUnmodifiableMap(
            Map.Entry::getKey,
            Map.Entry::getValue));
  }

  public Set<String> getLabelsSet() {
    return sensors.stream()
        .map(InternalSensors::getLabel)
        .collect(Collectors.toSet());
  }

  public Map<String, List<Double>> getLabelAndValuesFor(Set<String> labels) {
    return sensors.stream()
        .filter(s -> labels.contains(s.getLabel()))
        .collect(Collectors.groupingBy(
            InternalSensors::getLabel,
            HashMap::new,
            Collectors.mapping(
                InternalSensors::getValue,
                Collectors.toList())));
  }

  @Override
  public void sendSensorUpdate(String deviceId, double value, String metricLabel, double powerUsage) {
    sensors.add(new InternalSensors(deviceId, value, metricLabel, powerUsage));
  }

  @Override
  public void reportAnomaly(String deviceId, String errorType, String message) {
    System.out.println(">>> [ANALIZA MOCK] ! AWARIA ! " + deviceId + ": " + errorType);
  }
}
