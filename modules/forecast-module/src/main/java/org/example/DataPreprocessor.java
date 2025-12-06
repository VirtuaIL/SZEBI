package org.example;

import org.example.DTO.Odczyt;
import org.bson.Document;
import java.util.List;
import java.util.stream.Collectors;

public class DataPreprocessor {

    public static class ParsedData {
        public double moc;
        public double temperatura;

        public ParsedData(double moc, double temperatura) {
            this.moc = moc;
            this.temperatura = temperatura;
        }
    }

    public List<Odczyt> cleanData(List<Odczyt> rawData) {
        return rawData.stream()
                .filter(o -> o.getPomiary() != null && !o.getPomiary().isEmpty())
                .collect(Collectors.toList());
    }

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
}