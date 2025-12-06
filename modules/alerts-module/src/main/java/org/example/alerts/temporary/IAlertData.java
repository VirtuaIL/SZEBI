package org.example.alerts.temporary;

import java.util.List;

public interface IAlertData {
    void saveAlert(Alert alert);
    void updateAlertStatus(int id, AlertStatus status);
    List<Alert> getCriticalAlerts();
}