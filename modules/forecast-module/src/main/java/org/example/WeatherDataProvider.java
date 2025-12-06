package org.example;

import java.time.LocalDateTime;

public class WeatherDataProvider {

    public double getForecastTemperature(LocalDateTime dateTime) {
        return 10.0 + Math.random() * 15.0;
    }
}