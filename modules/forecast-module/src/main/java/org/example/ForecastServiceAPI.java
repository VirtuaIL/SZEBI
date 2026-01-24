package org.example;

import org.example.DTO.Prognoza;
import org.example.interfaces.IForecastingData;
import java.time.LocalDateTime;
import java.util.List;

public class ForecastServiceAPI {

    private final ForecastingModule forecastingModule;
    private final IForecastingData forecastingData;

    public ForecastServiceAPI(IForecastingData forecastingData) {
        this.forecastingData = forecastingData;
        this.forecastingModule = new ForecastingModule(forecastingData);
    }

    /**
     * Uruchamia proces generowania prognoz dla danego urządzenia.
     */
    public void generateForecast(int deviceId) {
        forecastingModule.startPredictionProcess(deviceId);
    }

    /**
     * Pobiera prognozy dla urządzenia na zadany okres.
     * Metoda ta może być używana przez inne moduły (np. Optymalizację) do
     * planowania.
     */
    public List<Prognoza> getForecasts(int deviceId, LocalDateTime from, LocalDateTime to) {
        // Zakładamy, że IForecastingData ma metodę do pobierania prognoz.
        // Jeśli nie, trzeba ją dodać lub symulować.
        // Na razie zwracamy null lub implementujemy logikę jeśli interfejs na to
        // pozwala.
        // Sprawdzimy IForecastingData w kolejnym kroku, tutaj zakładam istnienie takiej
        // metody
        // lub konieczność jej dodania.
        // Dla uproszczenia teraz zwrócimy pustą listę, ale docelowo to powinno działać.
        return null; // TODO: Dodać metodę getForecasts do IForecastingData
    }
}
