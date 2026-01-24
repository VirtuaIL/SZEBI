package org.example;

import org.example.DTO.Odczyt;
import org.bson.Document;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Klasa odpowiedzialna za przygotowanie danych.
 * Czyści, normalizuje i transformuje dane wejściowe na potrzeby modelu.
 */
public class DataPreprocessor {

    public static class ParsedData {
        public double moc;
        public double temperatura;

        public ParsedData(double moc, double temperatura) {
            this.moc = moc;
            this.temperatura = temperatura;
        }
    }
    
    private final ConfigManager config;
    private double meanMoc = 0.0;
    private double stdDevMoc = 1.0;
    private boolean normalizationCalculated = false;
    
    public DataPreprocessor() {
        this.config = ConfigManager.getInstance();
    }

    /**
     * Czyści dane - usuwa odczyty z pustymi pomiarami.
     */
    public List<Odczyt> cleanData(List<Odczyt> rawData) {
        return rawData.stream()
                .filter(o -> o.getPomiary() != null && !o.getPomiary().isEmpty())
                .collect(Collectors.toList());
    }
    
    /**
     * Czyści dane i wykrywa outlierów (jeśli włączone).
     */
    public List<Odczyt> cleanDataWithOutliers(List<Odczyt> rawData) {
        List<Odczyt> cleaned = cleanData(rawData);
        
        if (!config.isEnableOutlierDetection() || cleaned.isEmpty()) {
            return cleaned;
        }
        
        // Oblicz statystyki dla wykrywania outlierów
        List<Double> mocValues = new ArrayList<>();
        for (Odczyt o : cleaned) {
            ParsedData features = extractFeatures(o);
            if (features.moc > 0) {
                mocValues.add(features.moc);
            }
        }
        
        if (mocValues.size() < 3) {
            return cleaned; // Za mało danych
        }
        
        // Oblicz medianę i Interquartile Range
        List<Double> sorted = new ArrayList<>(mocValues);
        Collections.sort(sorted);
        
        double q1 = sorted.get(sorted.size() / 4);
        double q3 = sorted.get(sorted.size() * 3 / 4);
        double iqr = q3 - q1;
        double lowerBound = q1 - 1.5 * iqr;
        double upperBound = q3 + 1.5 * iqr;
        
        // Filtruj outlierów
        return cleaned.stream()
                .filter(o -> {
                    ParsedData features = extractFeatures(o);
                    return features.moc == 0 || (features.moc >= lowerBound && features.moc <= upperBound);
                })
                .collect(Collectors.toList());
    }

    /**
     * Wyciąga cechy z odczytu (moc i temperatura).
     */
    public ParsedData extractFeatures(Odczyt odczyt) {
        try {
            Document doc = Document.parse(odczyt.getPomiary());

            Double mocVal = 0.0;
            if (doc.containsKey("zuzycie_energii_W")) {
                mocVal = doc.get("zuzycie_energii_W", Number.class).doubleValue();
            }
            else if (doc.containsKey("moc")) {
                mocVal = doc.get("moc", Number.class).doubleValue();
            }

            Double tempVal = 0.0;
            if (doc.containsKey("temperatura_C")) {
                tempVal = doc.get("temperatura_C", Number.class).doubleValue();
            } else if (doc.containsKey("temp")) {
                tempVal = doc.get("temp", Number.class).doubleValue();
            }

            return new ParsedData(mocVal, tempVal);

        } catch (Exception e) {
            System.err.println("Błąd parsowania JSON w odczycie ID " + odczyt.getUrzadzenieId());
            return new ParsedData(0.0, 0.0);
        }
    }
    
    /**
     * Oblicza parametry normalizacji (średnia i odchylenie standardowe) na podstawie danych.
     */
    public void calculateNormalizationParams(List<Odczyt> data) {
        if (data == null || data.isEmpty()) {
            normalizationCalculated = false;
            return;
        }
        
        List<Double> mocValues = new ArrayList<>();
        for (Odczyt o : data) {
            ParsedData features = extractFeatures(o);
            if (features.moc > 0) {
                mocValues.add(features.moc);
            }
        }
        
        if (mocValues.isEmpty()) {
            normalizationCalculated = false;
            return;
        }
        
        // Oblicz średnią
        meanMoc = mocValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        // Oblicz odchylenie standardowe
        double variance = mocValues.stream()
                .mapToDouble(x -> Math.pow(x - meanMoc, 2))
                .average()
                .orElse(0.0);
        stdDevMoc = Math.sqrt(variance);
        
        // Unikaj dzielenia przez zero
        if (stdDevMoc == 0) {
            stdDevMoc = 1.0;
        }
        
        normalizationCalculated = true;
    }
    
    /**
     * Normalizuje wartość mocy (standaryzacja z-score).
     */
    public double normalizeMoc(double moc) {
        if (!config.isEnableNormalization() || !normalizationCalculated) {
            return moc;
        }
        return (moc - meanMoc) / stdDevMoc;
    }
    
    /**
     * Denormalizuje wartość mocy (odwrotna transformacja).
     */
    public double denormalizeMoc(double normalizedMoc) {
        if (!config.isEnableNormalization() || !normalizationCalculated) {
            return normalizedMoc;
        }
        return normalizedMoc * stdDevMoc + meanMoc;
    }
    
    /**
     * Dzieli dane na zbiór treningowy i walidacyjny.
     */
    public List<List<Odczyt>> splitTrainValidation(List<Odczyt> data) {
        if (data == null || data.isEmpty()) {
            return List.of(new ArrayList<>(), new ArrayList<>());
        }
        
        // Mieszanie danych dla lepszego podziału
        List<Odczyt> shuffled = new ArrayList<>(data);
        Collections.shuffle(shuffled);
        
        int splitIndex = (int) (shuffled.size() * config.getTrainValidationSplit());
        
        List<Odczyt> training = new ArrayList<>(shuffled.subList(0, splitIndex));
        List<Odczyt> validation = new ArrayList<>(shuffled.subList(splitIndex, shuffled.size()));
        
        return List.of(training, validation);
    }
}
