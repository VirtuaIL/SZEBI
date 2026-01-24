package org.example;

/**
 * Menedżer konfiguracji modułu prognozowania.
 * Zarządza parametrami pracy modułu i hiperparametrami modelu.
 */
public class ConfigManager {
    
    // Hiperparametry modelu
    private double minAccuracyThreshold = 0.10; // 10% MAPE
    private double trainValidationSplit = 0.8; // 80% train
    private int minTrainingSamples = 100; // Minimalna liczba próbek do treningu
    
    // Konfiguracja procesu prognozowania
    private int defaultForecastHorizon = 24; // Domyślny horyzont prognozy
    private int historicalDataDays = 7; // Liczba dni danych historycznych
    private boolean enableAutoRetraining = true; // Automatyczny retrening
    private int retrainingIntervalHours = 24; // Co ile godzin retrening
    
    // Konfiguracja preprocessing
    private boolean enableNormalization = true; // Włącz normalizację danych
    private boolean enableOutlierDetection = true; // Wykrywanie outlierów
    
    // Konfiguracja temperatury
    private double temperatureThreshold = 18.0; // Próg temperatury
    private double coldWeatherFactor = 1.2; // Współczynnik dla temp < threshold
    private double warmWeatherFactor = 0.9; // Współczynnik dla temp >= threshold
    
    // Singleton instance
    private static ConfigManager instance;
    
    private ConfigManager() {
        // Prywatny konstruktor dla singleton
    }
    
    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    // Getters i Setters dla hiperparametrów
    public double getMinAccuracyThreshold() {
        return minAccuracyThreshold;
    }
    
    public void setMinAccuracyThreshold(double minAccuracyThreshold) {
        if (minAccuracyThreshold < 0 || minAccuracyThreshold > 1) {
            throw new IllegalArgumentException("Próg dokładności musi być między 0 a 1");
        }
        this.minAccuracyThreshold = minAccuracyThreshold;
    }
    
    public double getTrainValidationSplit() {
        return trainValidationSplit;
    }
    
    public void setTrainValidationSplit(double trainValidationSplit) {
        if (trainValidationSplit <= 0 || trainValidationSplit >= 1) {
            throw new IllegalArgumentException("Podział train/validation musi być między 0 a 1");
        }
        this.trainValidationSplit = trainValidationSplit;
    }
    
    public int getMinTrainingSamples() {
        return minTrainingSamples;
    }
    
    public void setMinTrainingSamples(int minTrainingSamples) {
        if (minTrainingSamples < 1) {
            throw new IllegalArgumentException("Minimalna liczba próbek musi być >= 1");
        }
        this.minTrainingSamples = minTrainingSamples;
    }
    
    // Getters i Setters dla konfiguracji procesu
    public int getDefaultForecastHorizon() {
        return defaultForecastHorizon;
    }
    
    public void setDefaultForecastHorizon(int defaultForecastHorizon) {
        if (defaultForecastHorizon < 1) {
            throw new IllegalArgumentException("Horyzont prognozy musi być >= 1");
        }
        this.defaultForecastHorizon = defaultForecastHorizon;
    }
    
    public int getHistoricalDataDays() {
        return historicalDataDays;
    }
    
    public void setHistoricalDataDays(int historicalDataDays) {
        if (historicalDataDays < 1) {
            throw new IllegalArgumentException("Liczba dni danych historycznych musi być >= 1");
        }
        this.historicalDataDays = historicalDataDays;
    }
    
    public boolean isEnableAutoRetraining() {
        return enableAutoRetraining;
    }
    
    public void setEnableAutoRetraining(boolean enableAutoRetraining) {
        this.enableAutoRetraining = enableAutoRetraining;
    }
    
    public int getRetrainingIntervalHours() {
        return retrainingIntervalHours;
    }
    
    public void setRetrainingIntervalHours(int retrainingIntervalHours) {
        if (retrainingIntervalHours < 1) {
            throw new IllegalArgumentException("Interwał retreningu musi być >= 1");
        }
        this.retrainingIntervalHours = retrainingIntervalHours;
    }
    
    // Getters i Setters dla preprocessing
    public boolean isEnableNormalization() {
        return enableNormalization;
    }
    
    public void setEnableNormalization(boolean enableNormalization) {
        this.enableNormalization = enableNormalization;
    }
    
    public boolean isEnableOutlierDetection() {
        return enableOutlierDetection;
    }
    
    public void setEnableOutlierDetection(boolean enableOutlierDetection) {
        this.enableOutlierDetection = enableOutlierDetection;
    }
    
    // Getters i Setters dla temperatury
    public double getTemperatureThreshold() {
        return temperatureThreshold;
    }
    
    public void setTemperatureThreshold(double temperatureThreshold) {
        this.temperatureThreshold = temperatureThreshold;
    }
    
    public double getColdWeatherFactor() {
        return coldWeatherFactor;
    }
    
    public void setColdWeatherFactor(double coldWeatherFactor) {
        if (coldWeatherFactor <= 0) {
            throw new IllegalArgumentException("Współczynnik musi być > 0");
        }
        this.coldWeatherFactor = coldWeatherFactor;
    }
    
    public double getWarmWeatherFactor() {
        return warmWeatherFactor;
    }
    
    public void setWarmWeatherFactor(double warmWeatherFactor) {
        if (warmWeatherFactor <= 0) {
            throw new IllegalArgumentException("Współczynnik musi być > 0");
        }
        this.warmWeatherFactor = warmWeatherFactor;
    }
}
