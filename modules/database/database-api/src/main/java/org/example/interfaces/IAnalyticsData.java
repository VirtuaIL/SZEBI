package org.example.interfaces;

import java.time.LocalDateTime;
import java.util.List;

import org.example.DTO.AlertSzczegoly;
import org.example.DTO.Odczyt;
import org.example.DTO.Prognoza;
import org.example.DTO.Raport;

public interface IAnalyticsData {

    /**
     * Pobiera surowe, historyczne odczyty dla konkretnego urządzenia.
     * Podstawa do generowania szczegółowych wykresów.
     */
    List<Odczyt> getReadingsForDevice(int deviceId, LocalDateTime from, LocalDateTime to);

    /**
     * Pobiera surowe, historyczne odczyty dla wszystkich urządzeń w danym budynku.
     */
    List<Odczyt> getReadingsForBuilding(int buildingId, LocalDateTime from, LocalDateTime to);

    // --- Metody do pobierania DANYCH O ZDARZENIACH ---

    /**
     * Pobiera listę szczegółowych informacji o historycznych alertach dla danego budynku.
     */
    List<AlertSzczegoly> getAlertDetailsForBuilding(int buildingId, LocalDateTime from, LocalDateTime to);

    // --- Metody do analizy PROGNOZ ---

    /**
     * Pobiera zapisane w bazie wyniki prognoz dla danego urządzenia.
     * Umożliwia porównanie prognozy z rzeczywistością.
     */
    List<Prognoza> getForecastsForDevice(int deviceId, LocalDateTime from, LocalDateTime to);

    /**
     * Pobiera zapisane w bazie wyniki prognoz dla całego budynku.
     */
    List<Prognoza> getForecastsForBuilding(int buildingId, LocalDateTime from, LocalDateTime to);

    // --- Metody do AGREGACJI i obliczeń ---

    /**
     * Oblicza i zwraca sumaryczne zużycie energii dla danego budynku w określonym czasie.
     * Idealna do raportów kosztowych.
     */
    double getTotalEnergyConsumptionForBuilding(int buildingId, LocalDateTime from, LocalDateTime to);




    /**
     * Zapisuje wygenerowany raport w bazie danych.
     * @param report Obiekt Raport do zapisania.
     * @return Zapisany obiekt Raport z nadanym przez bazę ID.
     */
    Raport saveReport(Raport report);

    /**
     * Pobiera listę historycznych raportów danego typu.
     * @param reportType Typ raportu do wyszukania.
     * @return Lista obiektów Raport.
     */
    List<Raport> getReportsByType(String reportType);

    /**
     * Pobiera konkretny raport na podstawie jego ID.
     * @param reportId ID raportu.
     * @return Obiekt Raport lub null, jeśli nie znaleziono.
     */
    Raport getReportById(int reportId);
}