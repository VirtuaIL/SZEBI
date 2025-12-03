package org.example.DTO;

import java.time.LocalDateTime;

public class Alert {
    private int id;
    private int urzadzenieId;
    private String tresc;
    private LocalDateTime czasAlertu;

    // Gettery i Settery
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUrzadzenieId() { return urzadzenieId; }
    public void setUrzadzenieId(int urzadzenieId) { this.urzadzenieId = urzadzenieId; }
    public String getTresc() { return tresc; }
    public void setTresc(String tresc) { this.tresc = tresc; }
    public LocalDateTime getCzasAlertu() { return czasAlertu; }
    public void setCzasAlertu(LocalDateTime czasAlertu) { this.czasAlertu = czasAlertu; }
}
