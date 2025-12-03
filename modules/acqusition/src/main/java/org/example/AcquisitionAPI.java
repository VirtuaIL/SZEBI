package org.example;

public class AcquisitionAPI {
    private final CollectionService collectionService;
    private final DeviceManager deviceManager;

    public AcquisitionAPI(CollectionService collectionService, DeviceManager deviceManager) {
        this.collectionService = collectionService;
        this.deviceManager = deviceManager;
    }

    // Dodajemy parametr 'metricLabel'
    public void registerNewDevice(String id, String name, double min, double max, String metricLabel) {
        // Obliczamy średnią dla Mocka
        double expectedValue = (min + max) / 2.0;

        // Przekazujemy label do konstruktora Device
        Device newDevice = new Device(id, name, min, max, metricLabel, new MockDeviceConnector(expectedValue));

        deviceManager.addNewDevice(newDevice);
    }

    public void requestAllData() {
        System.out.println("[API] Użytkownik zażądał odczytu danych.");
        collectionService.forceRead();
    }
}