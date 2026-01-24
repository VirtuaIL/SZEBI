package org.example;

import org.example.DTO.Odczyt;
import org.example.DTO.Prognoza;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Konkretna implementacja algorytmu prognozującego.
 * Implementuje interfejs IForecastGenerator z logiką uczenia i predykcji.
 */
public class ForecastGenerator implements IForecastGenerator {
    
    private final DataPreprocessor preprocessor;
    private final ConfigManager config;
    
    public ForecastGenerator(DataPreprocessor preprocessor) {
        this.preprocessor = preprocessor;
        this.config = ConfigManager.getInstance();
    }
    
    @Override
    public ForecastModel train(List<Odczyt> trainingData, List<Odczyt> validationData) {
        ForecastModel model = new ForecastModel();
        
        if (trainingData == null || trainingData.isEmpty()) {
            System.out.println("[MODEL] Brak danych treningowych.");
            return model;
        }
        
        // Sprawdzenie minimalnej liczby próbek
        if (trainingData.size() < config.getMinTrainingSamples()) {
            System.out.println("[MODEL] Za mało danych treningowych: " + trainingData.size() + 
                             " (wymagane: " + config.getMinTrainingSamples() + ")");
            return model;
        }
        
        // Wyciąganie cech i obliczanie średniej
        double sum = 0.0;
        int count = 0;
        List<Double> trainingValues = new ArrayList<>();
        
        for (Odczyt o : trainingData) {
            DataPreprocessor.ParsedData features = preprocessor.extractFeatures(o);
            
            if (features.moc > 0) {
                trainingValues.add(features.moc);
                sum += features.moc;
                count++;
            }
        }
        
        if (count == 0) {
            System.out.println("[MODEL] Nie udało się wytrenować (brak dodatnich wartości zużycia).");
            return model;
        }
        
        double baselineConsumption = sum / count;
        model.setBaselineConsumption(baselineConsumption);
        model.setTrained(true);
        model.setTrainingTimestamp(LocalDateTime.now());
        model.setTrainingSamplesCount(count);
        
        // Walidacja jeśli dostępne dane walidacyjne
        if (validationData != null && !validationData.isEmpty()) {
            ModelMetrics metrics = evaluateOnValidation(model, validationData);
            model.setMetrics(metrics);
            
            System.out.println("[MODEL] Wytrenowano. Średnie zużycie bazowe: " + baselineConsumption);
            System.out.println("[MODEL] Metryki walidacyjne: " + metrics);
        } else {
            System.out.println("[MODEL] Wytrenowano. Średnie zużycie bazowe: " + baselineConsumption);
            System.out.println("[MODEL] Brak danych walidacyjnych - pominięto ocenę jakości.");
        }
        
        return model;
    }
    
    @Override
    public List<Prognoza> predict(ForecastModel model, int deviceId, LocalDateTime start, int hours, 
                                 WeatherDataProvider weatherProvider) {
        List<Prognoza> results = new ArrayList<>();
        
        if (model == null || !model.isTrained()) {
            System.out.println("[PREDICT] Model nie jest wytrenowany.");
            return results;
        }
        
        double baseline = model.getBaselineConsumption();
        double tempThreshold = config.getTemperatureThreshold();
        double coldFactor = config.getColdWeatherFactor();
        double warmFactor = config.getWarmWeatherFactor();
        
        for (int i = 1; i <= hours; i++) {
            LocalDateTime time = start.plusHours(i);
            double temp = weatherProvider.getForecastTemperature(time);
            
            // Korekta na podstawie temperatury
            double factor = (temp < tempThreshold) ? coldFactor : warmFactor;
            double predictedVal = baseline * factor;
            
            Prognoza p = new Prognoza();
            p.setUrzadzenieId(deviceId);
            p.setBudynekId(null);
            p.setCzasWygenerowania(LocalDateTime.now());
            p.setCzasPrognozy(time);
            p.setPrognozowanaWartosc(predictedVal);
            p.setMetryka("kWh");
            
            results.add(p);
        }
        
        return results;
    }
    
    @Override
    public ModelMetrics evaluate(List<Double> predictions, List<Double> actualValues) {
        if (predictions == null || actualValues == null || 
            predictions.size() != actualValues.size() || predictions.isEmpty()) {
            throw new IllegalArgumentException("Listy prognoz i wartości rzeczywistych muszą mieć taką samą długość i nie być puste");
        }
        
        int n = predictions.size();
        double sumAbsoluteError = 0.0;
        double sumSquaredError = 0.0;
        double sumPercentageError = 0.0;
        int validSamples = 0;
        
        for (int i = 0; i < n; i++) {
            double pred = predictions.get(i);
            double actual = actualValues.get(i);
            
            if (actual != 0) {
                double absoluteError = Math.abs(pred - actual);
                sumAbsoluteError += absoluteError;
                sumSquaredError += absoluteError * absoluteError;
                sumPercentageError += Math.abs((pred - actual) / actual);
                validSamples++;
            }
        }
        
        if (validSamples == 0) {
            return new ModelMetrics(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, 0);
        }
        
        double mae = sumAbsoluteError / validSamples;
        double rmse = Math.sqrt(sumSquaredError / validSamples);
        double mape = sumPercentageError / validSamples;
        
        return new ModelMetrics(mape, mae, rmse, validSamples);
    }
    
    /**
     * Pomocnicza metoda do oceny modelu na danych walidacyjnych.
     */
    private ModelMetrics evaluateOnValidation(ForecastModel model, List<Odczyt> validationData) {
        List<Double> predictions = new ArrayList<>();
        List<Double> actualValues = new ArrayList<>();

        double baseline = model.getBaselineConsumption();
        
        for (Odczyt o : validationData) {
            DataPreprocessor.ParsedData features = preprocessor.extractFeatures(o);
            
            if (features.moc > 0) {
                predictions.add(baseline);
                actualValues.add(features.moc);
            }
        }
        
        if (predictions.isEmpty()) {
            return new ModelMetrics(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, 0);
        }
        
        return evaluate(predictions, actualValues);
    }
}
