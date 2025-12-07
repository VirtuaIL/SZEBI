package org.example;

import org.example.DTO.Alert;
import org.example.DTO.Uzytkownik;
import org.example.DTO.Odczyt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        PostgresDataStorage storage = new PostgresDataStorage();

        System.out.println("\n--- Rozpoczynam test pobierania danych z MongoDB ---");

        LocalDateTime koniec = LocalDateTime.now();
        LocalDateTime poczatek = koniec.minusDays(7);

        int idUrzadzeniaDoTestu = 1; 
        System.out.println("Pobieranie odczytów dla urządzenia o ID: " + idUrzadzeniaDoTestu + " z ostatniego tygodnia.");

        List<Odczyt> historyczneOdczyty = storage.getReadingsForDevice(idUrzadzeniaDoTestu, poczatek, koniec);

        if (historyczneOdczyty.isEmpty()) {
            System.out.println("Nie znaleziono żadnych odczytów dla tego urządzenia w zadanym przedziale.");
        } else {
            System.out.println("Znaleziono " + historyczneOdczyty.size() + " odczytów:");
            for (Odczyt odczyt : historyczneOdczyty) {
                System.out.println("  - Czas: " + odczyt.getCzasOdczytu() + ", Pomiary: " + odczyt.getPomiary());
            }
        }

        System.out.println("--- Test pobierania danych z MongoDB zakończony ---");

    }
}