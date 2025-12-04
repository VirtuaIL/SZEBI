package org.example;

public class ErrorReporter {
    private final IAnalysisService analysisService;

    public ErrorReporter(IAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    public void reportCommunicationError(String deviceId, String message) {
        System.err.println("[ERROR] Błąd komunikacji [ID: " + deviceId + "]: " + message);

        if (analysisService != null) {
            analysisService.reportAnomaly(deviceId, "COMM_ERROR", message);
        }
    }

    public void reportDataValidationError(String deviceId, String message) {
        System.err.println("[WARN] Błąd walidacji [ID: " + deviceId + "]: " + message);

        if (analysisService != null) {
            analysisService.reportAnomaly(deviceId, "VALIDATION_ERROR", message);
        }
    }
}