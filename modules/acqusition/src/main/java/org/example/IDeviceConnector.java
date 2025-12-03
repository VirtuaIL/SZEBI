package org.example;

public interface IDeviceConnector {
    double readValue(); // Główny odczyt (np. temperatura)
    double getPowerUsage(); // Ile watów zużywa urządzenie
    boolean checkConnection();
}
