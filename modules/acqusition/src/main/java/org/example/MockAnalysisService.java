package org.example;

public class MockAnalysisService implements IAnalysisService {
    @Override
    public void reportAnomaly(String deviceId, String errorType, String message) {
        System.out.println(">>> [MODUŁ ANALIZY] Odebrano zgłoszenie awarii z " + deviceId + ": " + errorType);
        // cdn ...
    }
}