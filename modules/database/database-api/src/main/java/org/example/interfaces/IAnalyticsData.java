package org.example.interfaces;

import java.time.LocalDateTime;
import java.util.List;

import org.example.DTO.AlertSzczegoly;
import org.example.DTO.Odczyt;
import org.example.DTO.Prognoza;
import org.example.DTO.Raport;

public interface IAnalyticsData {


    List<Odczyt> getReadingsForDevice(int deviceId, LocalDateTime from, LocalDateTime to);

    List<Odczyt> getReadingsForBuilding(int buildingId, LocalDateTime from, LocalDateTime to);

    List<AlertSzczegoly> getAlertDetailsForBuilding(int buildingId, LocalDateTime from, LocalDateTime to);

    List<Prognoza> getForecastsForDevice(int deviceId, LocalDateTime from, LocalDateTime to);

    List<Prognoza> getForecastsForBuilding(int buildingId, LocalDateTime from, LocalDateTime to);

    double getTotalEnergyConsumptionForBuilding(int buildingId, LocalDateTime from, LocalDateTime to);

    Raport saveReport(Raport report);

    List<Raport> getReportsByType(String reportType);

    Raport getReportById(int reportId);
}