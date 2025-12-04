package org.example.DTO;

public class Urzadzenie {
    private int id;
    private int pokojId;
    private int modelId;
    private String parametryPracy; // JSON jako String
    private boolean aktywny;

    // Gettery i Settery
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getPokojId() { return pokojId; }
    public void setPokojId(int pokojId) { this.pokojId = pokojId; }
    public int getModelId() { return modelId; }
    public void setModelId(int modelId) { this.modelId = modelId; }
    public String getParametryPracy() { return parametryPracy; }
    public void setParametryPracy(String parametryPracy) { this.parametryPracy = parametryPracy; }
    public boolean isAktywny() { // Dla typów boolean, konwencja to 'is' zamiast 'get'
        return aktywny;
    }

    public void setAktywny(boolean aktywny) {
        this.aktywny = aktywny;
    }
}
