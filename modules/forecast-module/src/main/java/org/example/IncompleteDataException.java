package org.example;

/**
 * Wyjątek rzucany gdy brakuje wymaganych danych do wykonania operacji prognozowania.
 * Zgodnie z dokumentacją - administrator otrzymuje powiadomienie o braku danych.
 */
public class IncompleteDataException extends Exception {
    
    private final int deviceId;
    private final String reason;
    
    public IncompleteDataException(int deviceId, String reason) {
        super("Brak danych dla urządzenia " + deviceId + ": " + reason);
        this.deviceId = deviceId;
        this.reason = reason;
    }
    
    public IncompleteDataException(String message) {
        super(message);
        this.deviceId = 0;
        this.reason = message;
    }
    
    public int getDeviceId() {
        return deviceId;
    }
    
    public String getReason() {
        return reason;
    }
}
