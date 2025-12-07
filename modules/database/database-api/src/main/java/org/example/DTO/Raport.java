package org.example.DTO;

import java.time.LocalDateTime;

public class Raport {
    private int id;
    private Integer uzytkownikId;
    private LocalDateTime czasWygenerowania;
    private String typRaportu;
    private String opis;
    private LocalDateTime zakresOd;
    private LocalDateTime zakresDo;
    private String zawartosc; 

    // Gettery i Settery
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Integer getUzytkownikId() { return uzytkownikId; }
    public void setUzytkownikId(Integer uzytkownikId) { this.uzytkownikId = uzytkownikId; }
    public LocalDateTime getCzasWygenerowania() { return czasWygenerowania; }
    public void setCzasWygenerowania(LocalDateTime czasWygenerowania) { this.czasWygenerowania = czasWygenerowania; }
    public String getTypRaportu() { return typRaportu; }
    public void setTypRaportu(String typRaportu) { this.typRaportu = typRaportu; }
    public String getOpis() { return opis; }
    public void setOpis(String opis) { this.opis = opis; }
    public LocalDateTime getZakresOd() { return zakresOd; }
    public void setZakresOd(LocalDateTime zakresOd) { this.zakresOd = zakresOd; }
    public LocalDateTime getZakresDo() { return zakresDo; }
    public void setZakresDo(LocalDateTime zakresDo) { this.zakresDo = zakresDo; }
    public String getZawartosc() { return zawartosc; }
    public void setZawartosc(String zawartosc) { this.zawartosc = zawartosc; }
}
