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

    public List<Prognoza> getForecasts(int deviceId, LocalDateTime from, LocalDateTime to) {
        return analyticsData.getForecastsForDevice(deviceId, from, to);
    }

    public List<Prognoza> getForecastsForBuilding(int buildingId, LocalDateTime from, LocalDateTime to) {
        return analyticsData.getForecastsForBuilding(buildingId, from, to);
    }

    public ForecastModel getCurrentModel() {
        return forecastingModule.getCurrentModel();
    }

    public void retrainModel(int deviceId) throws IncompleteDataException {
        forecastingModule.retrainModel(deviceId);
    }
}
