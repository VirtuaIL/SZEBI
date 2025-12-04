package org.example;

public interface IAnalysisService {

    void sendSensorUpdate(String deviceId, double value, String metricLabel, double powerUsage);

    void reportAnomaly(String deviceId, String errorType, String message);
}