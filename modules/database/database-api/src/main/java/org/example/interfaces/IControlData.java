package org.example.interfaces;

import java.util.List;
import org.example.DTO.*;
import java.time.LocalDateTime;

public interface IControlData {

    Urzadzenie getDeviceById(int deviceId);

    List<Urzadzenie> getDevicesInRoom(int roomId);

    List<Pokoj> getRoomsInBuilding(int buildingId);

    // --- Metody do odpytywania o STAN i PREFERENCJE ---
    List<Odczyt> getReadingsForDevice(int deviceId, LocalDateTime from, LocalDateTime to);
    Uzytkownik getUserById(int userId); // Potrzebne do pobrania `preferencji`

    // --- Metody do odpytywania o OGRANICZENIA ---
    Umowa getActiveContractForBuilding(int buildingId);

    // --- Metody do STEROWANIA ---
    void updateDevice(Urzadzenie device);

}