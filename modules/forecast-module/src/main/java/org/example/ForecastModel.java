package org.example;

import org.example.DTO.Odczyt;
import org.example.DTO.Prognoza;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ForecastModel {
    private double baselineConsumption = 0.0;
    private boolean isTrained = false;
    private final DataPreprocessor preprocessor;

    public ForecastModel(DataPreprocessor preprocessor) {
        this.preprocessor = preprocessor;
    }

    public void train(List<Odczyt> data) {
        if (data.isEmpty()) return;

        double sum = 0;
        int count = 0;

        for (Odczyt o : data) {
            DataPreprocessor.ParsedData features = preprocessor.extractFeatures(o);

            if (features.moc > 0) {
                sum += features.moc;
                count++;
            }
        }

        if (count > 0) {
            this.baselineConsumption = sum / count;
            this.isTrained = true;
            System.out.println("[MODEL] Wytrenowano. Średnie zużycie bazowe: " + baselineConsumption);
        } else {
            System.out.println("[MODEL] Nie udało się wytrenować (brak dodatnich wartości zużycia).");
        }
    }

    public List<Prognoza> predict(int deviceId, LocalDateTime start, int hours, WeatherDataProvider weather) {
        List<Prognoza> results = new ArrayList<>();
        if (!isTrained) return results;

        for (int i = 1; i <= hours; i++) {
            LocalDateTime time = start.plusHours(i);
            double temp = weather.getForecastTemperature(time);

            double factor = (temp < 18.0) ? 1.2 : 0.9;
            double predictedVal = baselineConsumption * factor;

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
}