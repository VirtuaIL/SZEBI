package org.example;

public interface IAnalysisService {
    /**
     * Zgłasza anomalię lub awarię czujnika bezpośrednio do modułu analizy.
     * * @param deviceId ID urządzenia
     * @param errorType Typ błędu (np. "COMMUNICATION_ERROR", "VALIDATION_ERROR")
     * @param message Szczegóły
     */
    void reportAnomaly(String deviceId, String errorType, String message);
}