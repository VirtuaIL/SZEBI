package org.example.alerts;

import org.example.alerts.dto.ZgloszenieDTO;
import org.example.alerts.temporary.Alert;
import org.example.alerts.temporary.AlertStatus;
import org.example.alerts.temporary.AlertSeverity;
import org.example.alerts.temporary.IAlertData;

import java.util.Date;
import java.util.List;

public class AlertService {

    private final IAlertData alertRepository;
    private final NotificationService notificationService;

    public AlertService(IAlertData alertRepository) {
        this.alertRepository = alertRepository;
        this.notificationService = new NotificationService();
    }

    public void zglosAnomalie(ZgloszenieDTO dane) {
        System.out.println("[AlertService] Przetwarzanie zgłoszenia: " + dane.getTresc());

        Alert alert = new Alert();
        alert.setTresc(dane.getTresc());
        alert.setPriorytet(dane.getPriorytet());
        alert.setUrzadzenieId(dane.getDeviceId()); // Zmiana nazwy settera
        alert.setZrodlo(dane.getZrodlo());
        alert.setDataWystapienia(new Date());

        if (dane.getPriorytet() == AlertSeverity.CRITICAL || dane.getPriorytet() == AlertSeverity.WARNING) {
            alert.setStatus(AlertStatus.WYSLANY);
            notificationService.wyslijPush("AWARIA: " + alert.getTresc(), "Inżynier Utrzymania Ruchu");
        } else {
            alert.setStatus(AlertStatus.NOWY);
        }

        try {
            alertRepository.saveAlert(alert);
            System.out.println("[AlertService] Alert zapisany w bazie.");
        } catch (Exception e) {
            System.err.println("Błąd zapisu do bazy: " + e.getMessage());
        }
    }

    public List<Alert> pobierzAktywneAlarmy() {
        return alertRepository.getActiveAlertsBySeverity(AlertSeverity.CRITICAL);
    }


    public Alert getNewAlert(int deviceId) {

        List<Alert> alerts = alertRepository.getAlertsByDeviceId(deviceId);

        if (alerts == null || alerts.isEmpty()) {
            return null;
        }

        Alert latest = alerts.get(0);

        if (latest.getPriorytet() == AlertSeverity.CRITICAL &&
                latest.getStatus() != AlertStatus.ROZWIAZANY) {
            return latest;
        }

        return null;
    }

    public void potwierdzAlarm(int idAlertu) {
        System.out.println("[AlertService] Potwierdzanie alertu ID: " + idAlertu);
        alertRepository.updateAlertStatus(idAlertu, AlertStatus.POTWIERDZONY);
    }

    public void rozwiazAlarm(int idAlertu) {
        System.out.println("[AlertService] Zamykanie alertu ID: " + idAlertu);
        alertRepository.updateAlertStatus(idAlertu, AlertStatus.ROZWIAZANY);
    }
}