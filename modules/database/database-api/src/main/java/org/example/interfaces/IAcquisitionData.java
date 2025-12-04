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

    /**
     * Wykonuje szybki test ("ping") sprawdzający dostępność usług bazodanowych.
     * Kluczowe dla mechanizmu buforowania.
     * @return true, jeśli połączenie z bazą danych (np. PostgreSQL) jest aktywne, w przeciwnym razie false.
     */
    boolean isDatabaseConnected();
}