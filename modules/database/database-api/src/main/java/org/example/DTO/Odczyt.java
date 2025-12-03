package org.example.DTO;


import org.bson.Document;

import java.time.Instant;

public class Odczyt {
    private int urzadzenieId;
    private Instant czasOdczytu;
    private String pomiary; // Obiekt JSON jako String

    // Gettery i Settery
    public int getUrzadzenieId() { return urzadzenieId; }
    public void setUrzadzenieId(int urzadzenieId) { this.urzadzenieId = urzadzenieId; }
    public Instant getCzasOdczytu() { return czasOdczytu; }
    public void setCzasOdczytu(Instant czasOdczytu) { this.czasOdczytu = czasOdczytu; }
    public String getPomiary() { return pomiary; }
    public void setPomiary(String pomiary) { this.pomiary = pomiary; }

    public Document toDocument() {
        return new Document("id_urzadzenia", this.urzadzenieId)
                .append("czas_odczytu", this.czasOdczytu)
                .append("pomiary", Document.parse(this.pomiary)); // Parsujemy JSON string na obiekt BSON
    }
}
