package org.example.DTO;

// Plik: dto/AlertDetails.java

// Rozszerza klasę Alert, więc automatycznie ma wszystkie jej pola (id, tresc, status, etc.)
public class AlertSzczegoly extends Alert {

    // Dodatkowe pola, które pobierzemy za pomocą JOIN
    private String nazwaUrzadzenia;
    private String nazwaModelu;
    private String nazwaPokoju;
    private String nazwaBudynku;

    // Gettery i Settery dla nowych pól
    public String getNazwaUrzadzenia() { return nazwaUrzadzenia; }
    public void setNazwaUrzadzenia(String nazwaUrzadzenia) { this.nazwaUrzadzenia = nazwaUrzadzenia; }
    public String getNazwaModelu() { return nazwaModelu; }
    public void setNazwaModelu(String nazwaModelu) { this.nazwaModelu = nazwaModelu; }
    public String getNazwaPokoju() { return nazwaPokoju; }
    public void setNazwaPokoju(String nazwaPokoju) { this.nazwaPokoju = nazwaPokoju; }
    public String getNazwaBudynku() { return nazwaBudynku; }
    public void setNazwaBudynku(String nazwaBudynku) { this.nazwaBudynku = nazwaBudynku; }
}
