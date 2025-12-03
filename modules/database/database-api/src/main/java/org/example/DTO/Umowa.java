package org.example.DTO;

import java.time.LocalDate;

public class Umowa {
    private int id;
    private int budynekId;
    private int dostawcaId;
    private LocalDate dataPoczatku;
    private LocalDate dataKonca;
    private String szczegolyTaryfy; // JSON jako String

    // Gettery i Settery
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getBudynekId() { return budynekId; }
    public void setBudynekId(int budynekId) { this.budynekId = budynekId; }
    public int getDostawcaId() { return dostawcaId; }
    public void setDostawcaId(int dostawcaId) { this.dostawcaId = dostawcaId; }
    public LocalDate getDataPoczatku() { return dataPoczatku; }
    public void setDataPoczatku(LocalDate dataPoczatku) { this.dataPoczatku = dataPoczatku; }
    public LocalDate getDataKonca() { return dataKonca; }
    public void setDataKonca(LocalDate dataKonca) { this.dataKonca = dataKonca; }
    public String getSzczegolyTaryfy() { return szczegolyTaryfy; }
    public void setSzczegolyTaryfy(String szczegolyTaryfy) { this.szczegolyTaryfy = szczegolyTaryfy; }
}
