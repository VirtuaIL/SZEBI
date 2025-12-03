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
//        System.out.println("Rozpoczynam test modułu bazy danych...");

        // Stwórz instancję swojej klasy
        PostgresDataStorage storage = new PostgresDataStorage();

        System.out.println("\n--- Rozpoczynam test pobierania danych z MongoDB ---");

// Zdefiniuj przedział czasowy, dla którego chcesz pobrać dane
        LocalDateTime koniec = LocalDateTime.now();
        LocalDateTime poczatek = koniec.minusDays(7); // Ostatnie 7 dni

        int idUrzadzeniaDoTestu = 1; // Pobierzemy dane dla czujnika temperatury

        System.out.println("Pobieranie odczytów dla urządzenia o ID: " + idUrzadzeniaDoTestu + " z ostatniego tygodnia.");

// Wywołaj swoją zaimplementowaną metodę
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



//
//        Uzytkownik new1 = storage.getUserByEmail("admin@szebi.com");
//        System.out.println("Znaleziono użytkownika: " + new1.getImie() + " " + new1.getNazwisko());
//        new1.setEmail("admin1@szebi.com");
//        storage.saveUser(new1);


        // Stwórz przykładowy obiekt Alert
//        Alert testAlert = new Alert();
//        testAlert.setUrzadzenieId(1); // Upewnij się, że urządzenie o ID=1 istnieje w bazie!
//        testAlert.setTresc("To jest alert testowy z aplikacji Java.");
//        testAlert.setCzasAlertu(LocalDateTime.now());
//
//
//        // Wywołaj swoją zaimplementowaną metodę
//        storage.saveAlert(testAlert);
//
//        System.out.println("Test zakończony. Sprawdź tabelę 'Alerty' w bazie danych.");
    }
}