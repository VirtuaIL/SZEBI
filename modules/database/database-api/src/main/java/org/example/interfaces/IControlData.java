package org.example.interfaces;

import java.util.List;
import org.example.DTO.Urzadzenie;

public interface IControlData {

    Urzadzenie getDeviceById(int deviceId);

    List<Urzadzenie> getDevicesInRoom(int roomId);

    void updateDevice(Urzadzenie device);
}