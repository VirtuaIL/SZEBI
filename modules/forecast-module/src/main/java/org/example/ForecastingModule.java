package org.example;

import org.example.interfaces.IForecastingData;
import org.example.DTO.Odczyt;
import org.example.DTO.Prognoza;
import org.example.PostgresDataStorage;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Główny moduł sterujący procesem prognozowania.
 * Inicjuje uczenie, generuje prognozy i wysyła wyniki na zewnątrz.
 */
public class ForecastingModule {

    private final IForecastingData database;
    private final DataPreprocessor preprocessor;
    private final IForecastGenerator forecastGenerator;
    private final WeatherDataProvider weatherProvider;
    private final ConfigManager config;
    private ForecastModel currentModel;

    public ForecastingModule(IForecastingData database) {
        this.database = database;
        this.preprocessor = new DataPreprocessor();
        this.forecastGenerator = new ForecastGenerator(preprocessor);
        this.weatherProvider = new WeatherDataProvider();
        this.config = ConfigManager.getInstance();
        this.currentModel = new ForecastModel();
    }

    /**
     * Główna metoda rozpoczynająca proces prognozowania dla urządzenia.
     */
    public void startPredictionProcess(int deviceId) throws IncompleteDataException {
        System.out.println("\n>>> ROZPOCZYNAM PROCES DLA URZĄDZENIA ID: " + deviceId + " <<<");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime historicalStart = now.minusDays(config.getHistoricalDataDays());

        // Dane historyczne
        List<Odczyt> rawData = database.getReadingsForDevice(deviceId, historicalStart, now);

        if (rawData.isEmpty()) {
            String errorMsg = "Brak danych historycznych w bazie dla urządzenia " + deviceId;
            System.out.println("[WARN] " + errorMsg);
                throw new IncompleteDataException(deviceId, errorMsg);
        } else {
            System.out.println("[INFO] Pobrano " + rawData.size() + " odczytów.");
        }

        // Wyczyść dane
        List<Odczyt> cleanData;
        if (config.isEnableOutlierDetection()) {
            cleanData = preprocessor.cleanDataWithOutliers(rawData);
            System.out.println("[INFO] Po czyszczeniu i usunięciu outlierów: " + cleanData.size() + " odczytów.");
        } else {
            cleanData = preprocessor.cleanData(rawData);
            System.out.println("[INFO] Po czyszczeniu: " + cleanData.size() + " odczytów.");
        }

        if (cleanData.isEmpty()) {
            String errorMsg = "Brak danych po czyszczeniu dla urządzenia " + deviceId;
            System.out.println("[WARN] " + errorMsg);
            throw new IncompleteDataException(deviceId, errorMsg);
        }

        // Oblicz parametry normalizacji
        if (config.isEnableNormalization()) {
            preprocessor.calculateNormalizationParams(cleanData);
        }

        // Podziel dane na treningowe i walidacyjne
        List<List<Odczyt>> split = preprocessor.splitTrainValidation(cleanData);
        List<Odczyt> trainingData = split.get(0);
        List<Odczyt> validationData = split.get(1);

        System.out.println("[INFO] Podział danych: " + trainingData.size() + " treningowych, " + 
                          validationData.size() + " walidacyjnych.");

        // Trenuj nowy model
        ForecastModel newModel = forecastGenerator.train(trainingData, validationData);

        if (!newModel.isTrained()) {
            System.out.println("[WARN] Nie udało się wytrenować modelu.");
            return;
        }

        // Sprawdź czy nowy model jest lepszy niż obecny
        if (newModel.getMetrics() != null && currentModel.isTrained()) {
            double threshold = config.getMinAccuracyThreshold();
            
            if (newModel.getMetrics().isAcceptable(threshold)) {
                if (newModel.isBetterThan(currentModel)) {
                    System.out.println("[INFO] Nowy model jest lepszy - aktualizuję model produkcyjny.");
                    this.currentModel = newModel;
                } else {
                    System.out.println("[INFO] Nowy model nie jest lepszy - zachowuję poprzedni model.");
                }
            } else {
                System.out.println("[WARN] Nowy model nie osiągnął wymaganego progu dokładności (MAPE > " + 
                                 (threshold * 100) + "%). Zachowuję poprzedni model.");
                if (!currentModel.isTrained()) {
                    // Jeśli nie było poprzedniego modelu użyj nowego
                    this.currentModel = newModel;
                }
            }
        } else {
            this.currentModel = newModel;
        }

        // Generuj prognozy używając aktualnego modelu
        int forecastHorizon = config.getDefaultForecastHorizon();
        List<Prognoza> forecasts = forecastGenerator.predict(
            currentModel, deviceId, now, forecastHorizon, weatherProvider);

        if (forecasts.isEmpty()) {
            System.out.println("[WARN] Nie wygenerowano prognoz (model niewytrenowany?).");
            return;
        }

        // Zapisz prognozy do bazy
        int count = 0;
        for (Prognoza p : forecasts) {
            database.storeForecastResult(p);
            count++;
        }

        System.out.println("[SUKCES] Zapisano " + count + " prognoz dla urządzenia " + deviceId);
        
        // Wyślij wyniki do innych modułów
        sendResults(forecasts, deviceId);
    }

    private void sendResults(List<Prognoza> forecasts, int deviceId) {
        System.out.println("[INFO] Wyniki prognoz przekazane do innych modułów dla urządzenia " + deviceId);
        System.out.println("[INFO] Prognozy zapisane w bazie danych - dostępne dla modułu optymalizacji i sterowania");
        
        if (!forecasts.isEmpty()) {
            LocalDateTime firstForecast = forecasts.get(0).getCzasPrognozy();
            LocalDateTime lastForecast = forecasts.get(forecasts.size() - 1).getCzasPrognozy();
            System.out.println("[INFO] Zakres prognoz: " + firstForecast + " - " + lastForecast);
        }
    }

    /**
     * Zwraca aktualny wytrenowany model.
     */
    public ForecastModel getCurrentModel() {
        return currentModel;
    }

    /**
     * Wymusza retrening modelu.
     */
    public void retrainModel(int deviceId) throws IncompleteDataException {
        System.out.println("[INFO] Wymuszanie retreningu modelu dla urządzenia " + deviceId);
        startPredictionProcess(deviceId);
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
