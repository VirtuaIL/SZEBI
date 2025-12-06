package org.example;

import java.util.ArrayList;
import java.util.List;

public class AquisitionProxyService implements IAnalysisService {
  List<SealedSensor> sensors = new ArrayList<>();

  private class SealedSensor {
    SealedSensor(String id, double value, String label, double powerUsage) {
      this.id = id;
      this.value = value;
      this.label = label;
      this.powerUsage = powerUsage;
    }

    private String id;
    private double value;
    private String label;
    private double powerUsage;
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
