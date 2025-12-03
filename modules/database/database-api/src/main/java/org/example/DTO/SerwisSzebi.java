package org.example.DTO;

public class SerwisSzebi {
    private int id;
    private String nazwaFirmy;
    private String adres;
    private String email;
    private String telefon;

    // Gettery i Settery
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNazwaFirmy() { return nazwaFirmy; }
    public void setNazwaFirmy(String nazwaFirmy) { this.nazwaFirmy = nazwaFirmy; }
    public String getAdres() { return adres; }
    public void setAdres(String adres) { this.adres = adres; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefon() { return telefon; }
    public void setTelefon(String telefon) { this.telefon = telefon; }
}
