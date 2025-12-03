package org.example.DTO;

public class Budynek {
    private int id;
    private String nazwa;
    private String adres;
    private double powierzchnia;
    private int liczbaPieter;
    private int serwisId;

    // Gettery i Settery
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNazwa() { return nazwa; }
    public void setNazwa(String nazwa) { this.nazwa = nazwa; }
    public String getAdres() { return adres; }
    public void setAdres(String adres) { this.adres = adres; }
    public double getPowierzchnia() { return powierzchnia; }
    public void setPowierzchnia(double powierzchnia) { this.powierzchnia = powierzchnia; }
    public int getLiczbaPieter() { return liczbaPieter; }
    public void setLiczbaPieter(int liczbaPieter) { this.liczbaPieter = liczbaPieter; }
    public int getSerwisId() { return serwisId; }
    public void setSerwisId(int serwisId) { this.serwisId = serwisId; }
}
