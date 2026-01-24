package org.example;

import java.util.Random;

public class MockDeviceConnector implements IDeviceConnector {
    private final Random random = new Random();
    private double baseValue;
    private final double powerUsage; // Konkretna moc z bazy danych

    // Nowy konstruktor przyjmujący moc z bazy
    public MockDeviceConnector(double startingValue, double powerUsage) {
        this.baseValue = startingValue;
        this.powerUsage = powerUsage;
    }

    @Override
    public double readValue() {
        double noise = (random.nextDouble() * 0.2) - 0.1;
        return baseValue + noise;
    }

    @Override
    public boolean setValue(Double newValue) {
        baseValue = newValue;
        return true;
    }

    @Override
    public double getPowerUsage() {
        // Symulujemy lekkie wahania zużycia energii (np. +/- 1%)
        // Ale bazujemy na PRAWDZIWEJ mocy urządzenia z bazy danych
        return powerUsage + (powerUsage * (random.nextDouble() * 0.02 - 0.01));
    }

    @Override
    public boolean checkConnection() {
        return true;
    }
}