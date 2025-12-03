package org.example.interfaces;

import java.time.LocalDateTime;
import java.util.List;
import org.example.DTO.Odczyt;

public interface IForecastingData {

    List<Odczyt> getReadingsForDevice(int deviceId, LocalDateTime from, LocalDateTime to);

    List<Odczyt> getReadingsForBuilding(int buildingId, LocalDateTime from, LocalDateTime to);
}