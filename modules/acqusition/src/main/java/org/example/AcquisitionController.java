package org.example;

import org.example.DTO.UrzadzenieSzczegoly;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AcquisitionController {
    IAnalysisService analysisService = new MockAnalysisService();
    PostgresDataStorage databaseStorage = new PostgresDataStorage();
    DataCollector dataCollector = new DataCollector(databaseStorage);
    DeviceManager deviceManager = new DeviceManager(databaseStorage);
    ErrorReporter errorReporter = new ErrorReporter(analysisService);
    CollectionService service = new CollectionService(deviceManager, dataCollector, errorReporter, analysisService);
    AcquisitionAPI api = new AcquisitionAPI(service, deviceManager, dataCollector, analysisService);

    @GetMapping("/api/getDeviceList")
    public String getDeviceList() {
        List<UrzadzenieSzczegoly> devicesFromDb = databaseStorage.getActiveDevicesWithDetails();
        System.out.println("ACTIVE DEVICES IN CONTROLLER: " + deviceManager.getActiveDevices());
        return devicesFromDb.toString();
    }

    @GetMapping("/api/startPeriodicCollectionTask")
    public String getString2() {
        service.runPeriodicCollectionTask();
        return "Periodic collection task started";
    }

    @GetMapping("/api/getDeviceReadings")
    public String requestSensorRead(@RequestParam(name = "id") String deviceID) {
        Double result = api.requestSensorRead(deviceID);
        System.out.println("ID supplied: " + deviceID);
        return result.toString();
    }
}
