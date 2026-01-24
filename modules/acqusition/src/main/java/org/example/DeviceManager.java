package org.example;

import org.example.interfaces.IAcquisitionData;

import org.example.DTO.Urzadzenie;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DeviceManager {
    private final IAcquisitionData dataStorage;

    public DeviceManager(IAcquisitionData dataStorage) {
        this.dataStorage = dataStorage;
    }

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

    public Boolean saveDeviceToDB(JSONObject parameters, int roomID, int modelID){
        Urzadzenie dbDevice = new Urzadzenie();
        dbDevice.setParametryPracy(parameters.toString());
        dbDevice.setPokojId(roomID);
        dbDevice.setModelId(modelID);

        dataStorage.addDevice(dbDevice);
        return true;
    }
}