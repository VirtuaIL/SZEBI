package org.example.DTO;

public class UrzadzenieSzczegoly extends Urzadzenie{
    // I dodaje nowe pola na nazwy, które pobierzemy za pomocą JOIN
    private String nazwaTypu;
    private String nazwaProducenta;
    private String nazwaModelu;
    private String nazwaPokoju;

    private Double minRange;
    private Double maxRange;
    private String metricLabel;

    private Integer mocW;          // Odpowiada "moc_W"
    private Boolean sciemnialna;   // Odpowiada "sciemnialna"
    private Integer barwaK;

    // Gettery i Settery dla nowych pól...
    public String getNazwaTypu() { return nazwaTypu; }
    public void setNazwaTypu(String nazwaTypu) { this.nazwaTypu = nazwaTypu; }
    public String getNazwaProducenta() { return nazwaProducenta; }
    public void setNazwaProducenta(String nazwaProducenta) { this.nazwaProducenta = nazwaProducenta; }
    public String getNazwaModelu() { return nazwaModelu; }
    public void setNazwaModelu(String nazwaModelu) { this.nazwaModelu = nazwaModelu; }
    public String getNazwaPokoju() { return nazwaPokoju; }
    public void setNazwaPokoju(String nazwaPokoju) { this.nazwaPokoju = nazwaPokoju; }

    public Double getMinRange() { return minRange; }
    public void setMinRange(Double minRange) { this.minRange = minRange; }
    public Double getMaxRange() { return maxRange; }
    public void setMaxRange(Double maxRange) { this.maxRange = maxRange; }
    public String getMetricLabel() { return metricLabel; }
    public void setMetricLabel(String metricLabel) { this.metricLabel = metricLabel; }

    public Integer getMocW() { return mocW; }
    public void setMocW(Integer mocW) { this.mocW = mocW; }

    public Boolean getSciemnialna() { return sciemnialna; }
    public void setSciemnialna(Boolean sciemnialna) { this.sciemnialna = sciemnialna; }

    public Integer getBarwaK() { return barwaK; }
    public void setBarwaK(Integer barwaK) { this.barwaK = barwaK; }
}
