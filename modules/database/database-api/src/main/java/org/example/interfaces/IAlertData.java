package org.example.interfaces;

import java.util.List;
import org.example.DTO.Alert;
import org.example.DTO.AlertSzczegoly;

public interface IAlertData {

    void saveAlert(Alert alert);

    List<Alert> getAlertsByDeviceId(int deviceId);

    void updateAlertStatus(int alertId, Alert.AlertStatus newStatus);

    /**
     * Pobiera wszystkie alerty dla danego budynku.
     */
    List<Alert> getAlertsForBuilding(int buildingId);

    /**
     * Pobiera wszystkie aktywne (nierozwiązane) alerty o zadanym priorytecie.
     * Używamy teraz zagnieżdżonego enuma Alert.AlertSeverity.
     */
    List<Alert> getActiveAlertsBySeverity(Alert.AlertSeverity severity);

    /**
     * Pobiera listę szczegółowych informacji o alertach dla całego budynku.
     */
    List<AlertSzczegoly> getAlertDetailsForBuilding(int buildingId);

    /**
     * Pobiera listę szczegółowych informacji o alertach dla jednego, konkretnego pokoju.
     */
    List<AlertSzczegoly> getAlertDetailsForRoom(int roomId);

    /**
     * Pobiera szczegółowe informacje dla jednego, konkretnego alertu.
     * Użyteczne do wyświetlenia widoku szczegółów alertu.
     */
    AlertSzczegoly getAlertDetailsById(int alertId);
}
