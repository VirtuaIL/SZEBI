package org.example;

import java.time.LocalDateTime;

/**
 * Struktura danych reprezentująca wytrenowany model prognozujący.
 * Przechowuje parametry modelu, metryki jakości i status treningu.
 */
public class ForecastModel {
    private double baselineConsumption = 0.0;
    private boolean isTrained = false;
    private ModelMetrics metrics; // Metryki jakości modelu
    private LocalDateTime trainingTimestamp; // Kiedy model został wytrenowany
    private int trainingSamplesCount; // Liczba próbek użytych do treningu
    
    public ForecastModel() {
        // Pusty konstruktor dla nowego modelu
    }
    
    public ForecastModel(double baselineConsumption, boolean isTrained, ModelMetrics metrics, 
                        LocalDateTime trainingTimestamp, int trainingSamplesCount) {
        this.baselineConsumption = baselineConsumption;
        this.isTrained = isTrained;
        this.metrics = metrics;
        this.trainingTimestamp = trainingTimestamp;
        this.trainingSamplesCount = trainingSamplesCount;
    }

    public double getBaselineConsumption() {
        return baselineConsumption;
    }
    
    public void setBaselineConsumption(double baselineConsumption) {
        this.baselineConsumption = baselineConsumption;
    }
    
    public boolean isTrained() {
        return isTrained;
    }
    
    public void setTrained(boolean trained) {
        isTrained = trained;
    }
    
    public ModelMetrics getMetrics() {
        return metrics;
    }
    
    public void setMetrics(ModelMetrics metrics) {
        this.metrics = metrics;
    }
    
    public LocalDateTime getTrainingTimestamp() {
        return trainingTimestamp;
    }
    
    public void setTrainingTimestamp(LocalDateTime trainingTimestamp) {
        this.trainingTimestamp = trainingTimestamp;
    }
    
    public int getTrainingSamplesCount() {
        return trainingSamplesCount;
    }
    
    public void setTrainingSamplesCount(int trainingSamplesCount) {
        this.trainingSamplesCount = trainingSamplesCount;
    }
    
    /**
     * Sprawdza czy model jest lepszy niż inny model na podstawie MAPE.
     */
    public boolean isBetterThan(ForecastModel other) {
        if (other == null || other.metrics == null) {
            return this.metrics != null;
        }
        if (this.metrics == null) {
            return false;
        }
        return this.metrics.getMape() < other.metrics.getMape();
    }
}
