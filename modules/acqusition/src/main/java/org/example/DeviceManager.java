package org.example;

import java.util.ArrayList;
import java.util.List;

public class DeviceManager {
    private List<Device> activeDevices = new ArrayList<>();

    public void addNewDevice(Device device) {
        activeDevices.add(device);
        System.out.println("[MANAGER] Zarejestrowano urządzenie: " + device.getId());
    }

    public List<Device> getActiveDevices() {
        return activeDevices;
    }

    public Device getDeviceById(String id) {
        for (Device d : activeDevices) {
            if (d.getId().equals(id)) return d;
        }
        return null;
    }
}