package org.example.interfaces;

import org.example.DTO.Odczyt;
import org.example.DTO.Urzadzenie;
import java.util.List;
import org.example.DTO.UrzadzenieSzczegoly;

public interface IAcquisitionData {

    void saveSensorReading(Odczyt reading);

    void saveBatchSensorReadings(List<Odczyt> readings);

    List<Urzadzenie> getActiveDevices();

    List<UrzadzenieSzczegoly> getActiveDevicesWithDetails();


    boolean isDatabaseConnected();
}