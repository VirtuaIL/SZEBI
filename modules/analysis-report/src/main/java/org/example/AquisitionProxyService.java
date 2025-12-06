package org.example;

import java.util.*;
import java.util.stream.Collectors;

public class AquisitionProxyService implements IAnalysisService {
  List<SealedSensor> sensors = new ArrayList<>();

  private class SealedSensor {
    private final String id;
    private final double value;
    private final String label;
    private final double powerUsage;

    SealedSensor(String id, double value, String label, double powerUsage) {
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

  public HashMap<String, List<Double>> getLabelValues() {
    System.out.println(sensors);
    return sensors.stream()
        .collect(Collectors.groupingBy(
            SealedSensor::getLabel,
            HashMap::new,
            Collectors.mapping(
                SealedSensor::getValue,
                Collectors.toList())));
  }

  @Override
  public void sendSensorUpdate(String deviceId, double value, String metricLabel, double powerUsage) {
    sensors.add(new SealedSensor(deviceId, value, metricLabel, powerUsage));
  }

  @Override
  public void reportAnomaly(String deviceId, String errorType, String message) {
    System.out.println(">>> [ANALIZA MOCK] ! AWARIA ! " + deviceId + ": " + errorType);
  }
}
