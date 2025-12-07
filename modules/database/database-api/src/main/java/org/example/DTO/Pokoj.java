package org.example.DTO;

public class Pokoj {
    private int id;
    private int budynekId;
    private String numerPokoju;
    private int pietro;
    private java.util.List<Integer> uzytkownicyIds = new java.util.ArrayList<>();

    // Gettery i Settery
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBudynekId() {
        return budynekId;
    }

    public void setBudynekId(int budynekId) {
        this.budynekId = budynekId;
    }

    public String getNumerPokoju() {
        return numerPokoju;
    }

    public void setNumerPokoju(String numerPokoju) {
        this.numerPokoju = numerPokoju;
    }

    public int getPietro() {
        return pietro;
    }

    public void setPietro(int pietro) {
        this.pietro = pietro;
    }

    public java.util.List<Integer> getUzytkownicyIds() {
        return uzytkownicyIds;
    }

    public void setUzytkownicyIds(java.util.List<Integer> uzytkownicyIds) {
        this.uzytkownicyIds = uzytkownicyIds;
    }
}
