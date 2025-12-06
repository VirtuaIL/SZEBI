package org.example;

public class OZEResource {
    private double producedEnergy;
    private String sourceType;

    public OZEResource(double producedEnergy, String sourceType) {
        this.producedEnergy = producedEnergy;
        this.sourceType = sourceType;
    }

    public double getProducedEnergy() {
        return producedEnergy;
    }

    public void setProducedEnergy(double producedEnergy) {
        this.producedEnergy = producedEnergy;
    }

    public void updateProduction(double newValue) {
        this.producedEnergy = newValue;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
}
