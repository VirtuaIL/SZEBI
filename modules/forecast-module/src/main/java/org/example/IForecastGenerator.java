package org.example;

import org.example.DTO.Odczyt;
import org.example.DTO.Prognoza;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Interfejs definiujący kontrakt dla algorytmów prognozujących.
 * Zapewnia elastyczność w implementacji różnych modeli ML.
 */
public interface IForecastGenerator {
    
    /**
     * Trenuje model na podstawie danych historycznych.
     * @param trainingData Dane treningowe
     * @param validationData Dane walidacyjne (może być null)
     * @return Wytrenowany model z metrykami
     */
    ForecastModel train(List<Odczyt> trainingData, List<Odczyt> validationData);
    
    /**
     * Generuje prognozy na podstawie wytrenowanego modelu.
     * @param model Wytrenowany model
     * @param deviceId ID urządzenia
     * @param start Czas rozpoczęcia prognozy
     * @param hours Horyzont czasowy prognozy (w godzinach)
     * @param weatherProvider Dostawca danych pogodowych
     * @return Lista prognoz
     */
    List<Prognoza> predict(ForecastModel model, int deviceId, LocalDateTime start, int hours, WeatherDataProvider weatherProvider);
    
    /**
     * Oblicza metryki jakości modelu (np. MAPE, MAE, RMSE).
     * @param predictions Prognozy
     * @param actualValues Rzeczywiste wartości
     * @return Obiekt z metrykami
     */
    ModelMetrics evaluate(List<Double> predictions, List<Double> actualValues);
}
