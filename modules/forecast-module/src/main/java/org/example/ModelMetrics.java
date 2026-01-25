package org.example;

/**
 * Klasa przechowująca metryki jakości modelu prognozującego.
 */
public class ModelMetrics {
    private double mape; // Średni bezwzględny błąd procentowy
    private double mae;  // Średni bezwzględny błąd
    private double rmse; // Pierwiastek średniego błędu kwadratowego
    private int sampleCount; // Liczba próbek użytych do oceny
    
    public ModelMetrics(double mape, double mae, double rmse, int sampleCount) {
        this.mape = mape;
        this.mae = mae;
        this.rmse = rmse;
        this.sampleCount = sampleCount;
    }
    
    public double getMape() {
        return mape;
    }
    
    public double getMae() {
        return mae;
    }
    
    public double getRmse() {
        return rmse;
    }
    
    public int getSampleCount() {
        return sampleCount;
    }
    
    public boolean isAcceptable(double threshold) {
        return mape <= threshold;
    }
    
    @Override
    public String toString() {
        return String.format("ModelMetrics{MAPE=%.2f%%, MAE=%.2f, RMSE=%.2f, samples=%d}", 
            mape * 100, mae, rmse, sampleCount);
    }
}
