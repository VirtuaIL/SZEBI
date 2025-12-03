package org.example.interfaces;

import org.example.DTO.Odczyt;
import java.util.List;

public interface IAcquisitionData {

    void saveSensorReading(Odczyt reading);

    void saveBatchSensorReadings(List<Odczyt> readings);
}