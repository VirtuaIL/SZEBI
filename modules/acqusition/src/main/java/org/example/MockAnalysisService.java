package org.example;

public class MockAnalysisService implements IAnalysisService {

    @Override
    public void sendSensorUpdate(String deviceId, double value, String metricLabel, double powerUsage) {
        System.out.printf(">>> [ANALIZA MOCK] Dane: ID=%s | Val=%.2f %s | Moc=%.2f W%n",
                deviceId, value, metricLabel, powerUsage);
    }

    @Override
    public void reportAnomaly(String deviceId, String errorType, String message) {
        System.out.println(">>> [ANALIZA MOCK] ! AWARIA ! " + deviceId + ": " + errorType);
    }
}
