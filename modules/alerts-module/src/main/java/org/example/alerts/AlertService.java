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

    // Wstrzykiwanie zależności (Dependency Injection)
    public AlertService(IAlertData alertRepository) {
        this.alertRepository = alertRepository;
        this.notificationService = new NotificationService();
    }

    // Metoda z diagramu sekwencji: zglosAnomalie [cite: 95]
    public void zglosAnomalie(ZgloszenieDTO dane) {
        System.out.println("[AlertService] Przetwarzanie zgłoszenia: " + dane.getTresc());

        // 1. Mapowanie DTO na encję Alert
        Alert alert = new Alert();
        alert.setTresc(dane.getTresc());
        alert.setPriorytet(dane.getPriorytet());
        alert.setDeviceId(dane.getDeviceId());
        alert.setZrodlo(dane.getZrodlo());
        alert.setDataWystapienia(new Date());
        alert.setStatus(AlertStatus.NOWY);

        // 2. Zapis do bazy danych [cite: 97]
        try {
            alertRepository.saveAlert(alert);
            System.out.println("[AlertService] Alert zapisany w bazie.");
        } catch (Exception e) {
            System.err.println("Błąd zapisu do bazy: " + e.getMessage());
        }

        // 3. Wysyłanie powiadomienia (dla statusów CRITICAL/WARNING) [cite: 99]
        if (dane.getPriorytet() == AlertSeverity.CRITICAL || dane.getPriorytet() == AlertSeverity.WARNING) {
            // Zmiana statusu na WYSLANY
            alert.setStatus(AlertStatus.WYSLANY);
            // alertRepository.updateAlertStatus(alert.getId(), AlertStatus.WYSLANY); // Opcjonalnie aktualizacja w bazie

            notificationService.wyslijPush(
                    "AWARIA: " + alert.getTresc(),
                    "Inżynier Utrzymania Ruchu" // Aktor z diagramu [cite: 94]
            );
        }
    }

    // Metoda wymagana przez UI i Moduł Optymalizacji [cite: 13, 137]
    public List<Alert> pobierzAktywneAlarmy() {
        return alertRepository.getCriticalAlerts();
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