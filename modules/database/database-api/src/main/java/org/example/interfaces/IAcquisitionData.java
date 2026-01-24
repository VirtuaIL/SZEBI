package org.example.interfaces;

import org.example.DTO.*;

import java.util.List;

public interface IAcquisitionData {

    void saveSensorReading(Odczyt reading);

    void saveBatchSensorReadings(List<Odczyt> readings);

    List<Urzadzenie> getActiveDevices();

    List<UrzadzenieSzczegoly> getActiveDevicesWithDetails();

    List<ProducentUrzadzenia> getAvailableManufacturers();

    List<ModelUrzadzenia> getAvailableModels();

    Boolean addDevice(Urzadzenie device);

    boolean isDatabaseConnected();
}