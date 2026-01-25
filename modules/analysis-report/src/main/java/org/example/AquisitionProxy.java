package org.example;

import java.util.*;
import java.util.stream.Collectors;

public class AquisitionProxy implements IAnalysisService {
  List<InternalSensors> sensors = new ArrayList<>();

  private class InternalSensors {
    private final String id;
    private final double value;
    private final DeviceType label;
    private final double powerUsage;

    InternalSensors(String id, double value, DeviceType label, double powerUsage) {
      this.id = id;
      this.value = value;
      this.label = label;
      this.powerUsage = powerUsage;
    }

    ConfigurationType getLabel() {
      return ConfigurationType.fromDeviceType(label);
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

  public Map<ConfigurationType, List<Double>> getLabelValues() {
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

  public Set<ConfigurationType> getLabelsSet() {
    System.out.println(">>> [ANALIZA MOCK] getLabelsSet"
        + sensors.stream().map(InternalSensors::getLabel).collect(Collectors.toSet()));
    return sensors.stream()
        .map(InternalSensors::getLabel)
        .filter(Objects::nonNull) // Filtrujemy nule
        .collect(Collectors.toSet());
  }

  public Map<ConfigurationType, List<Double>> getLabelAndValuesFor(HashSet<ConfigurationType> labels) {
    return sensors.stream()
        .filter(s -> labels.contains(s.getLabel()))
        .collect(Collectors.groupingBy(
            InternalSensors::getLabel,
            HashMap::new,
            Collectors.mapping(
                InternalSensors::getValue,
                Collectors.toList())));
  }

  public List<SensorReading> getSensorReadingsFor(HashSet<ConfigurationType> labels) {
    return sensors.stream()
        .filter(s -> s.getLabel() != null && labels.contains(s.getLabel()))
        .map(s -> new SensorReading(s.getId(), s.getValue(), s.getLabel()))
        .collect(Collectors.toList());
  }

  @Override
  public void sendSensorUpdate(String deviceId, double value, DeviceType metricLabel, double powerUsage) {
    sensors.add(new InternalSensors(deviceId, value, metricLabel, powerUsage));
  }

  @Override
  public void reportAnomaly(String deviceId, String errorType, String message) {
    System.out.println(">>> [ANALIZA MOCK] ! AWARIA ! " + deviceId + ": " + errorType);
  }
}
