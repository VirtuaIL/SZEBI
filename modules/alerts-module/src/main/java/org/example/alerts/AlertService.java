package org.example.alerts;

import org.example.alerts.dto.ZgloszenieDTO;
import org.example.interfaces.IAlertData;
import org.example.DTO.Alert;
import org.example.DTO.Alert.AlertStatus;
import org.example.DTO.Alert.AlertSeverity;

import java.time.LocalDateTime;
import java.util.List;

public class AlertService {

    private final IAlertData alertRepository;
    private final NotificationService notificationService;

    // Teraz konstruktor przyjmuje PRAWDZIWY interfejs IAlertData
    public AlertService(IAlertData alertRepository) {
        this.alertRepository = alertRepository;
        this.notificationService = new NotificationService();
    }

    public void zglosAnomalie(ZgloszenieDTO dane) {
        System.out.println("[AlertService] Przetwarzanie zgłoszenia: " + dane.getTresc());

        Alert alert = new Alert();
        alert.setTresc(dane.getTresc());
        alert.setPriorytet(dane.getPriorytet());
        alert.setUrzadzenieId(dane.getDeviceId());
        alert.setCzasAlertu(LocalDateTime.now());

        if (dane.getPriorytet() == AlertSeverity.CRITICAL || dane.getPriorytet() == AlertSeverity.WARNING) {
            alert.setStatus(AlertStatus.NOWY);

            notificationService.wyslijPush("AWARIA: " + alert.getTresc(), "Inżynier Utrzymania Ruchu");
        } else {
            alert.setStatus(AlertStatus.NOWY);
        }

        try {
            alertRepository.saveAlert(alert);
            System.out.println("[AlertService] Alert zapisany w bazie.");
        } catch (Exception e) {
            System.err.println("Błąd zapisu do bazy: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<Alert> pobierzAktywneAlarmy() {
        return alertRepository.getActiveAlertsBySeverity(AlertSeverity.CRITICAL);
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