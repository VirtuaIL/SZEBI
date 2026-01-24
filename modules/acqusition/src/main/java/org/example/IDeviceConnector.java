package org.example;

public interface IDeviceConnector {
    double readValue();
    double getPowerUsage();
    boolean checkConnection();
    boolean setValue(Double newValue);
}
