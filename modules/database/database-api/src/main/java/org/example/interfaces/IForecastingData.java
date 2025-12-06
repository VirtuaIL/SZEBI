package org.example.interfaces;

import java.time.LocalDateTime;
import java.util.List;
import org.example.DTO.Odczyt;
import org.example.DTO.Prognoza;

public interface IForecastingData {

    List<Odczyt> getReadingsForDevice(int deviceId, LocalDateTime from, LocalDateTime to);

    List<Odczyt> getReadingsForBuilding(int buildingId, LocalDateTime from, LocalDateTime to);

    /**
     * Zapisuje wynik działania algorytmu prognozującego w bazie danych.
     * @param forecast Obiekt Prognoza zawierający przewidywaną wartość dla danego czasu.
     */
    void storeForecastResult(Prognoza forecast);
}