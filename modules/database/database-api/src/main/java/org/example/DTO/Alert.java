package org.example.DTO;

import java.time.LocalDateTime;

public class Alert {

    public enum AlertStatus {
        NOWY,
        POTWIERDZONY,
        ROZWIAZANY
    }

    public enum AlertSeverity {
        INFO,
        WARNING,
        CRITICAL
    }

    private int id;
    private int urzadzenieId;
    private String tresc;
    private LocalDateTime czasAlertu;

    private AlertStatus status;
    private AlertSeverity priorytet;

    // Gettery i Settery
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUrzadzenieId() { return urzadzenieId; }
    public void setUrzadzenieId(int urzadzenieId) { this.urzadzenieId = urzadzenieId; }
    public String getTresc() { return tresc; }
    public void setTresc(String tresc) { this.tresc = tresc; }
    public LocalDateTime getCzasAlertu() { return czasAlertu; }
    public void setCzasAlertu(LocalDateTime czasAlertu) { this.czasAlertu = czasAlertu; }

    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { this.status = status; }
    public AlertSeverity getPriorytet() { return priorytet; }
    public void setPriorytet(AlertSeverity priorytet) { this.priorytet = priorytet; }
}
