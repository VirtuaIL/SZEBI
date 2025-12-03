package org.example.DTO;

public class ModelUrzadzenia {
    private int id;
    private int typId;
    private int producentId;
    private String nazwaModelu;

    // Gettery i Settery
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getTypId() { return typId; }
    public void setTypId(int typId) { this.typId = typId; }
    public int getProducentId() { return producentId; }
    public void setProducentId(int producentId) { this.producentId = producentId; }
    public String getNazwaModelu() { return nazwaModelu; }
    public void setNazwaModelu(String nazwaModelu) { this.nazwaModelu = nazwaModelu; }
}
