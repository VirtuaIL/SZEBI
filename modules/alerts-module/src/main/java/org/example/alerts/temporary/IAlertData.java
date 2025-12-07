package org.example.alerts.temporary;

import java.util.List;

public interface IAlertData {
    void saveAlert(Alert alert);
    void updateAlertStatus(int alertId, AlertStatus newStatus);
    List<Alert> getActiveAlertsBySeverity(AlertSeverity severity);
    List<Alert> getAlertsByDeviceId(int deviceId);
}