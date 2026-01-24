package org.example;

public interface IAnalysisService {

  void sendSensorUpdate(String deviceId, double value, DeviceType metricLabel, double powerUsage);

  void reportAnomaly(String deviceId, String errorType, String message);
}
