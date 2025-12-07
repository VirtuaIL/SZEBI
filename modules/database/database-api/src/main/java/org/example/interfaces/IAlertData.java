package org.example.interfaces;

import java.util.List;
import org.example.DTO.Alert;
import org.example.DTO.AlertSzczegoly;

public interface IAlertData {

    void saveAlert(Alert alert);

    List<Alert> getAlertsByDeviceId(int deviceId);

    void updateAlertStatus(int alertId, Alert.AlertStatus newStatus);

    List<Alert> getAlertsForBuilding(int buildingId);

    List<Alert> getActiveAlertsBySeverity(Alert.AlertSeverity severity);

    List<AlertSzczegoly> getAlertDetailsForBuilding(int buildingId);

    List<AlertSzczegoly> getAlertDetailsForRoom(int roomId);

    AlertSzczegoly getAlertDetailsById(int alertId);
}
