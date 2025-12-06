package org.example;

import org.example.interfaces.IForecastingData;
import org.example.DTO.Odczyt;
import org.example.DTO.Prognoza;
import org.example.PostgresDataStorage;

import java.time.LocalDateTime;
import java.util.List;

public class ForecastingModule {

    private final IForecastingData database;
    private final DataPreprocessor preprocessor;
    private final ForecastModel model;
    private final WeatherDataProvider weatherProvider;

    public ForecastingModule(IForecastingData database) {
        this.database = database;
        this.preprocessor = new DataPreprocessor();
        this.model = new ForecastModel(preprocessor);
        this.weatherProvider = new WeatherDataProvider();
    }

    public void startPredictionProcess(int deviceId) {
        System.out.println("\n>>> ROZPOCZYNAM PROCES DLA URZĄDZENIA ID: " + deviceId + " <<<");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);

        List<Odczyt> rawData = database.getReadingsForDevice(deviceId, sevenDaysAgo, now);

        if (rawData.isEmpty()) {
            System.out.println("[WARN] Urządzenie ID " + deviceId + ": Brak danych historycznych w bazie.");
            return;
        } else {
            System.out.println("[INFO] Pobrano " + rawData.size() + " odczytów.");
        }

        List<Odczyt> cleanData = preprocessor.cleanData(rawData);

        model.train(cleanData);

        List<Prognoza> forecasts = model.predict(deviceId, now, 24, weatherProvider);

        if (forecasts.isEmpty()) {
            System.out.println("[WARN] Nie wygenerowano prognoz (model niewytrenowany?).");
            return;
        }

        int count = 0;
        for (Prognoza p : forecasts) {
            database.storeForecastResult(p);
            count++;
        }

        System.out.println("[SUKCES] Zapisano " + count + " prognoz dla urządzenia " + deviceId);
    }

    public static void main(String[] args) {
        System.out.println("--- SYSTEM PROGNOZOWANIA SZEBI ---");

        PostgresDataStorage realDatabase = new PostgresDataStorage();

        ForecastingModule module = new ForecastingModule(realDatabase);

        int[] deviceIds = {1, 2, 3};

        for (int id : deviceIds) {
            try {
                module.startPredictionProcess(id);
            } catch (Exception e) {
                System.err.println("Błąd krytyczny dla urządzenia " + id + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("\n--- ZAKOŃCZONO WSZYSTKIE OPERACJE ---");
    }
}