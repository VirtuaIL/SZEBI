package org.example;

public class ErrorReporter {
    private final IAnalysisService analysisService;

    // Wstrzykujemy zależność do zewnętrznego modułu
    public ErrorReporter(IAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    public void reportCommunicationError(String deviceId, String message) {
        System.err.println("[LOG LOKALNY] Błąd komunikacji: " + message);
        // Wysyłamy sygnał do Modułu Analizy
        if (analysisService != null) {
            analysisService.reportAnomaly(deviceId, "CONNECTION_LOST", message);
        }
    }

    public void reportDataValidationError(String deviceId, String message) {
        System.err.println("[LOG LOKALNY] Błąd walidacji: " + message);
        // Wysyłamy sygnał do Modułu Analizy
        if (analysisService != null) {
            analysisService.reportAnomaly(deviceId, "VALUE_OUT_OF_RANGE", message);
        }
    }
}