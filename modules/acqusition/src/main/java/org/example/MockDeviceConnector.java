package org.example;
import java.util.Random;

public class MockDeviceConnector implements IDeviceConnector {
    private final Random random = new Random();
    private final double baseValue;

    // Ile prądu zużywa ten czujnik (np. 0.1 W - 2.0 W)
    private final double powerUsageBase;

    public MockDeviceConnector(double startingValue) {
        this.baseValue = startingValue;
        // Losujemy zużycie prądu dla danego typu urządzenia (małe wartości)
        this.powerUsageBase = 0.1 + (random.nextDouble() * 0.5);
    }

    @Override
    public double readValue() {
        return baseValue + (random.nextDouble() * 5.0) - 2.5;
    }

    @Override
    public double getPowerUsage() {
        // Symulujemy lekkie wahania zużycia energii przez samą elektronikę
        return powerUsageBase + (random.nextDouble() * 0.05);
    }

    @Override
    public boolean checkConnection() { return true; }
}