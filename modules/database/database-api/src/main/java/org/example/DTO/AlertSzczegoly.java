package org.example.DTO;

public class AlertSzczegoly extends Alert {


    private String nazwaUrzadzenia;
    private String nazwaModelu;
    private String nazwaPokoju;
    private String nazwaBudynku;

    public String getNazwaUrzadzenia() { return nazwaUrzadzenia; }
    public void setNazwaUrzadzenia(String nazwaUrzadzenia) { this.nazwaUrzadzenia = nazwaUrzadzenia; }
    public String getNazwaModelu() { return nazwaModelu; }
    public void setNazwaModelu(String nazwaModelu) { this.nazwaModelu = nazwaModelu; }
    public String getNazwaPokoju() { return nazwaPokoju; }
    public void setNazwaPokoju(String nazwaPokoju) { this.nazwaPokoju = nazwaPokoju; }
    public String getNazwaBudynku() { return nazwaBudynku; }
    public void setNazwaBudynku(String nazwaBudynku) { this.nazwaBudynku = nazwaBudynku; }
}
