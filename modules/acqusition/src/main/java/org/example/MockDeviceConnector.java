package org.example;

import java.util.Random;

public class MockDeviceConnector implements IDeviceConnector {
    private final Random random = new Random();
    private double baseValue;
    private final double powerUsage; // Konkretna moc z bazy danych

    private double powerPerUnitFactor;

    // Nowy konstruktor przyjmujący moc z bazy
    public MockDeviceConnector(double startingValue, double powerUsage) {
        this.baseValue = startingValue;
        this.powerUsage = powerUsage;

        if (startingValue != 0)
        {
            double estimatedMaxRange = startingValue * 2.0;
            this.powerPerUnitFactor = powerUsage / estimatedMaxRange;
        }
        else
        {
            this.powerPerUnitFactor = 0;
        }
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
        double currentRawPower = baseValue * powerPerUnitFactor;
        double absPower = Math.abs(currentRawPower);
        return absPower + (absPower * (random.nextDouble() * 0.02 - 0.01));
    }

    @Override
    public boolean checkConnection() {
        return true;
    }
}