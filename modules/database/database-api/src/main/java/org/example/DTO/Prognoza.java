package org.example.DTO;

import java.time.LocalDateTime;

public class Prognoza {
    private int id;
    private Integer urzadzenieId; // Może być null, jeśli prognoza dotyczy budynku
    private Integer budynekId;   // Może być null, jeśli prognoza dotyczy urządzenia
    private LocalDateTime czasWygenerowania;
    private LocalDateTime czasPrognozy;
    private double prognozowanaWartosc;
    private String metryka;

    // Gettery i Settery
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Integer getUrzadzenieId() { return urzadzenieId; }
    public void setUrzadzenieId(Integer urzadzenieId) { this.urzadzenieId = urzadzenieId; }
    public Integer getBudynekId() { return budynekId; }
    public void setBudynekId(Integer budynekId) { this.budynekId = budynekId; }
    public LocalDateTime getCzasWygenerowania() { return czasWygenerowania; }
    public void setCzasWygenerowania(LocalDateTime czasWygenerowania) { this.czasWygenerowania = czasWygenerowania; }
    public LocalDateTime getCzasPrognozy() { return czasPrognozy; }
    public void setCzasPrognozy(LocalDateTime czasPrognozy) { this.czasPrognozy = czasPrognozy; }
    public double getPrognozowanaWartosc() { return prognozowanaWartosc; }
    public void setPrognozowanaWartosc(double prognozowanaWartosc) { this.prognozowanaWartosc = prognozowanaWartosc; }
    public String getMetryka() { return metryka; }
    public void setMetryka(String metryka) { this.metryka = metryka;}

}
