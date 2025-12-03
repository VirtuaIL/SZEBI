package org.example.interfaces;

import java.time.LocalDateTime;
import java.util.List;
import org.example.DTO.Odczyt;

public interface IAnalyticsData {

    List<Odczyt> getReadingsForDevice(int deviceId, LocalDateTime from, LocalDateTime to);

    double getTotalEnergyConsumptionForBuilding(int buildingId, LocalDateTime from, LocalDateTime to);
}