package org.example;

import org.example.DTO.Prognoza;
import org.example.interfaces.IForecastingData;
import org.example.interfaces.IAnalyticsData;
import org.example.interfaces.IAcquisitionData;
import java.time.LocalDateTime;
import java.util.List;

public class ForecastServiceAPI {

    private final ForecastingModule forecastingModule;
    private final IForecastingData forecastingData;
    private final IAnalyticsData analyticsData;
    private ForecastScheduler scheduler;

    public ForecastServiceAPI(IForecastingData forecastingData, IAnalyticsData analyticsData) {
        this.forecastingData = forecastingData;
        this.analyticsData = analyticsData;
        this.forecastingModule = new ForecastingModule(forecastingData);
    }
    
    public void initializeScheduler(IAcquisitionData acquisitionData) {
        if (scheduler == null) {
            this.scheduler = new ForecastScheduler(this, acquisitionData);
        }
    }
    
    public void startScheduler() {
        if (scheduler != null) {
            scheduler.start();
        }
    }
    
    public void stopScheduler() {
        if (scheduler != null) {
            scheduler.stop();
        }
    }

    public void generateForecast(int deviceId) throws IncompleteDataException {
        try {
            forecastingModule.startPredictionProcess(deviceId);
        } catch (IncompleteDataException e) {
            throw e;
        } catch (Exception e) {
            throw new IncompleteDataException(deviceId, "Błąd podczas generowania prognoz: " + e.getMessage());
        }
    }

    /**
     * Pobiera prognozy dla urządzenia na zadany okres.
     * Metoda ta może być używana przez inne moduły (np. Optymalizację) do planowania.
     * @param deviceId ID urządzenia
     * @param from Data początkowa
     * @param to Data końcowa
     * @return Lista prognoz dla danego okresu
     */
    public List<Prognoza> getForecasts(int deviceId, LocalDateTime from, LocalDateTime to) {
        return analyticsData.getForecastsForDevice(deviceId, from, to);
    }

    /**
     * Pobiera prognozy dla budynku na zadany okres.
     * @param buildingId ID budynku
     * @param from Data początkowa
     * @param to Data końcowa
     * @return Lista prognoz dla danego okresu
     */
    public List<Prognoza> getForecastsForBuilding(int buildingId, LocalDateTime from, LocalDateTime to) {
        return analyticsData.getForecastsForBuilding(buildingId, from, to);
    }

    /**
     * Zwraca aktualny wytrenowany model.
     * @return Wytrenowany model lub null jeśli nie jest wytrenowany
     */
    public ForecastModel getCurrentModel() {
        return forecastingModule.getCurrentModel();
    }

    /**
     * Wymusza retrening modelu dla urządzenia.
     * @param deviceId ID urządzenia
     */
    public void retrainModel(int deviceId) throws IncompleteDataException {
        forecastingModule.retrainModel(deviceId);
    }
}
