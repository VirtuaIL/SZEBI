CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- --- KROK 1: TWORZENIE TABEL (w kolejności zależności) ---

-- Tabele niezależne
CREATE TABLE Rola (ID_roli SERIAL PRIMARY KEY, Nazwa_roli VARCHAR(255), Opis_roli TEXT);
CREATE TABLE Serwis_systemu_SZEBI (ID_serwisu SERIAL PRIMARY KEY, Nazwa_firmy VARCHAR(255), Adres TEXT, Email VARCHAR(255), Telefon VARCHAR(50));
CREATE TABLE Dostawca_Energii (ID_dostawcy SERIAL PRIMARY KEY, Nazwa_firmy VARCHAR(255));
CREATE TABLE Typ_urzadzenia (ID_typu_urzadzenia SERIAL PRIMARY KEY, nazwa_typu_urzadzenia VARCHAR(255));
CREATE TABLE Producent_urzadzenia (ID_producenta SERIAL PRIMARY KEY, nazwa_producenta VARCHAR(255));
CREATE TABLE Budynek (ID_budynku SERIAL PRIMARY KEY, Nazwa VARCHAR(255), Adres TEXT, Powierzchnia NUMERIC, Liczba_pieter INTEGER);




-- Tabele zależne
CREATE TABLE Uzytkownik (ID_uzytkownika SERIAL PRIMARY KEY, ID_roli INTEGER REFERENCES Rola(ID_roli), Imie VARCHAR(255), Nazwisko VARCHAR(255), Telefon VARCHAR(50), Email VARCHAR(255), Haslo_hash VARCHAR(255), preferencje JSONB);
CREATE TABLE Umowa (ID_umowy SERIAL PRIMARY KEY, ID_budynku INTEGER REFERENCES Budynek(ID_budynku), ID_dostawcy INTEGER REFERENCES Dostawca_Energii(ID_dostawcy), data_poczatku DATE, data_konca DATE, szczegoly_taryfy JSONB);
CREATE TABLE Pokoje (ID_pokoju SERIAL PRIMARY KEY, ID_budynku INTEGER REFERENCES Budynek(ID_budynku), numer_pokoju VARCHAR(50), pietro INTEGER);
CREATE TABLE Model_urzadzenia (ID_modelu SERIAL PRIMARY KEY, ID_typu_urzadzenia INTEGER REFERENCES Typ_urzadzenia(ID_typu_urzadzenia), ID_producenta INTEGER REFERENCES Producent_urzadzenia(ID_producenta), nazwa_modelu VARCHAR(100));
--CREATE TABLE Urzadzenia (ID_urzadzenia SERIAL PRIMARY KEY, ID_pokoju INTEGER REFERENCES Pokoje(ID_pokoju), ID_modelu INTEGER REFERENCES Model_urzadzenia(ID_modelu), Parametry_pracy JSONB);
CREATE TABLE Urzadzenia (
    ID_urzadzenia SERIAL PRIMARY KEY,
    ID_pokoju INTEGER NOT NULL REFERENCES Pokoje(ID_pokoju),
    ID_modelu INTEGER NOT NULL REFERENCES Model_urzadzenia(ID_modelu),
    Parametry_pracy JSONB,
    -- NOWA, WAŻNA KOLUMNA:
    aktywny BOOLEAN NOT NULL DEFAULT true -- Domyślnie każde nowe urządzenie jest aktywne
);
CREATE TABLE Alerty (
    ID_alertu SERIAL PRIMARY KEY, 
    ID_urzadzenia INTEGER REFERENCES Urzadzenia(ID_urzadzenia), 
    priorytet VARCHAR(50) NOT NULL, -- Przechowa "INFO", "WARNING", "CRITICAL"
    status VARCHAR(50) NOT NULL DEFAULT 'NOWY', -- Przechowa "NOWY", "POTWIERDZONY", "ROZWIAZANY"
    
    tresc TEXT NOT NULL,
    czas_alertu TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE TABLE Prognozy (
    ID_prognozy SERIAL PRIMARY KEY,
    
    -- Informacje o tym, czego dotyczy prognoza
    ID_urzadzenia INTEGER REFERENCES Urzadzenia(ID_urzadzenia), -- Prognoza dla konkretnego urządzenia
    ID_budynku INTEGER REFERENCES Budynek(ID_budynku),       -- lub dla całego budynku
    
    -- Czas, kiedy prognoza została wygenerowana
    czas_wygenerowania TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Czas, którego dotyczy prognoza
    czas_prognozy TIMESTAMPTZ NOT NULL,
    
    -- Przewidywana wartość
    prognozowana_wartosc NUMERIC NOT NULL,
    
    -- Metadane (np. typ prognozy, nazwa modelu ML)
    metryka VARCHAR(100) NOT NULL -- np. "zuzycie_energii_kwh", "temperatura_C"
);
CREATE INDEX idx_prognozy_urzadzenia ON Prognozy(ID_urzadzenia, czas_prognozy);
CREATE INDEX idx_prognozy_budynku ON Prognozy(ID_budynku, czas_prognozy);

CREATE TABLE Raporty (
    ID_raportu SERIAL PRIMARY KEY,
    
    -- Kto wygenerował raport
    ID_uzytkownika INTEGER REFERENCES Uzytkownik(ID_uzytkownika),
    
    -- Czas wygenerowania
    czas_wygenerowania TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Metadane raportu
    typ_raportu VARCHAR(100) NOT NULL, -- np. "zuzycie_energii_miesieczne", "historia_alertow"
    opis TEXT,
    
    -- Zakres czasowy, którego dotyczy raport
    zakres_od TIMESTAMPTZ NOT NULL,
    zakres_do TIMESTAMPTZ NOT NULL,
    
    -- Sam raport - używamy JSONB, aby przechowywać ustrukturyzowane wyniki
    zawartosc JSONB NOT NULL
);

-- Indeksy dla szybszego wyszukiwania
CREATE INDEX idx_raporty_uzytkownika ON Raporty(ID_uzytkownika);
CREATE INDEX idx_raporty_typ ON Raporty(typ_raportu);


-- =============================================================================
-- == KROK 2: WYPEŁNIENIE DANYCH (w poprawnej kolejności)
-- =============================================================================

-- 1. Wypełnij tabele słownikowe (bez zależności)
INSERT INTO Rola (Nazwa_roli, Opis_roli) VALUES ('Administrator', 'Zarządza całym systemem'), ('Inżynier', 'Monitoruje stan techniczny'), ('Najemca', 'Standardowy użytkownik');
INSERT INTO Serwis_systemu_SZEBI (Nazwa_firmy, Adres, Email, Telefon) VALUES ('SZEBI Software House', 'ul. Wirtualna 1, Warszawa', 'support@szebi.com', '111-222-333');
INSERT INTO Dostawca_Energii (Nazwa_firmy) VALUES ('PGE'), ('Tauron'), ('Enea');
INSERT INTO Typ_urzadzenia (nazwa_typu_urzadzenia) VALUES ('Czujnik Temperatury'), ('Klimatyzator'), ('Oświetlenie LED');
INSERT INTO Producent_urzadzenia (nazwa_producenta) VALUES ('Siemens'), ('Samsung'), ('Philips');

-- 2. Stwórz byty nadrzędne
INSERT INTO Budynek (Nazwa, Adres, Powierzchnia, Liczba_pieter) VALUES ('Biurowiec Alfa', 'Warszawa, ul. Główna 1', 2500, 5), ('Biurowiec Beta', 'Kraków, ul. Boczna 2', 4500, 8), ('Magazyn Gamma', 'Poznań, ul. Logistyczna 3', 12000, 1);
INSERT INTO Uzytkownik (ID_roli, Imie, Nazwisko, Telefon, Email, Haslo_hash, preferencje) VALUES (1, 'Adam', 'Adminowski', '100100100', 'admin@szebi.com', '...hash...', '{ "preferowana_temperatura": 19.0 }'), (2, 'Ignacy', 'Inżynierski', '200200200', 'inzynier@szebi.com', '...hash...', '{ "preferowana_temperatura": 25.0 }'), (3, 'Natalia', 'Nowak', '300300300', 'natalia.nowak@szebi.com', '...hash...', '{ "preferowana_temperatura": 22.0 }');

-- 3. Stwórz byty zależne (dzieci)
INSERT INTO Pokoje (ID_budynku, numer_pokoju, pietro) VALUES (1, '101', 1), (1, '102', 1), (2, 'Serwerownia', 0), (3, 'Hala Główna', 1);
INSERT INTO Umowa (ID_budynku, ID_dostawcy, data_poczatku, data_konca, szczegoly_taryfy) VALUES (1, 1, '2024-01-01', '2025-12-31', '{ "waluta": "PLN", "taryfy": [{ "nazwa": "dzienna", "cena_za_kwh": 0.85 }] }'), (2, 2, '2023-07-01', '2024-06-30', '{ "waluta": "PLN", "taryfy": [{ "nazwa": "standard", "cena_za_kwh": 0.90 }] }'), (3, 3, '2024-03-01', NULL, '{ "waluta": "PLN", "taryfy": [{ "nazwa": "weekendowa", "cena_za_kwh": 0.60 }] }');
INSERT INTO Model_urzadzenia (ID_typu_urzadzenia, ID_producenta, nazwa_modelu) VALUES (1, 1, 'Climatix T1'), (2, 2, 'WindFree Avant'), (3, 3, 'Hue White E27');
INSERT INTO Urzadzenia (ID_pokoju, ID_modelu, Parametry_pracy) VALUES 
(1, 1, '{ "moc_W": 9, "zakres_pomiaru": { "min": -20.0, "max": 50.0 }, "etykieta_metryki": "temperatura_C" }'), 
(2, 3, '{ "moc_W": 10, "sciemnialna": true, "barwa_K": 2700 }'), 
(3, 2, '{ "moc_W": 11, "zakres_pomiaru": { "min": 16.0, "max": 30.0 }, "etykieta_metryki": "temperatura_C" }'), 
(4, 1, '{ "moc_W": 12, "zakres_pomiaru": { "min": -40.0, "max": 60.0 }, "etykieta_metryki": "temperatura_C" }');
INSERT INTO Alerty (ID_urzadzenia, priorytet, status, tresc) VALUES
(1, 'CRITICAL', 'NOWY', 'Temperatura w pokoju 101 przekroczyła próg alarmowy!'),
(2, 'WARNING', 'NOWY', 'Wysoki poziom wilgotności w Serwerowni.'),
(3, 'INFO', 'ROZWIAZANY', 'Utracono i odzyskano komunikację z lampą w pokoju 102.');
INSERT INTO Prognozy (ID_urzadzenia, ID_budynku, czas_prognozy, prognozowana_wartosc, metryka)
VALUES
(
    1,       -- ID_urzadzenia (prognoza dla konkretnego czujnika)
    NULL,    -- ID_budynku jest puste
    '2025-12-10 14:00:00', -- Czas, którego dotyczy prognoza
    21.5,    -- Przewidywana temperatura
    'temperatura_C' -- Typ prognozy
),
(
    NULL,    -- ID_urzadzenia jest puste
    1,       -- ID_budynku (prognoza dla całego biurowca)
    '2025-12-11 00:00:00', -- Czas, którego dotyczy prognoza (np. cały następny dzień)
    1570.5,  -- Przewidywane sumaryczne zużycie energii w kWh na cały dzień
    'zuzycie_energii_kwh' -- Typ prognozy
);
INSERT INTO Raporty (ID_uzytkownika, czas_wygenerowania, typ_raportu, opis, zakres_od, zakres_do, zawartosc)
VALUES
(
    2, -- Wygenerowany przez Inżyniera (ID=2)
    '2025-12-01 09:00:00', -- Czas wygenerowania raportu
    'zuzycie_energii_miesieczne',
    'Miesięczne podsumowanie zużycia energii dla Biurowca Alfa',
    '2025-11-01 00:00:00', -- Początek okresu raportowania
    '2025-11-30 23:59:59', -- Koniec okresu raportowania
    '{
      "id_budynku": 1,
      "nazwa_budynku": "Biurowiec Alfa",
      "podsumowanie": {
        "calkowite_zuzycie_kwh": 12540.78,
        "srednie_dzienne_zuzycie_kwh": 418.02,
        "przewidywany_koszt_pln": 10659.66
      },
      "zuzycie_per_pokoj": [
        { "id_pokoju": 1, "numer_pokoju": "101", "zuzycie_kwh": 1200.5 },
        { "id_pokoju": 2, "numer_pokoju": "102", "zuzycie_kwh": 980.2 }
      ]
    }'::jsonb
);

-- Raport 2: Tygodniowy raport o alertach dla "Biurowca Beta" (ID=2)
INSERT INTO Raporty (ID_uzytkownika, czas_wygenerowania, typ_raportu, opis, zakres_od, zakres_do, zawartosc)
VALUES
(
    2, -- Wygenerowany przez Inżyniera (ID=2)
    '2025-11-28 16:00:00',
    'historia_alertow_tygodniowa',
    'Tygodniowy raport o alertach dla Biurowca Beta',
    '2025-11-21 00:00:00',
    '2025-11-28 00:00:00',
    '{
      "id_budynku": 2,
      "nazwa_budynku": "Biurowiec Beta",
      "podsumowanie": {
        "liczba_alertow": 5,
        "liczba_krytycznych": 1,
        "liczba_ostrzezen": 4
      },
      "najczestsze_urzadzenie_alertujace": {
        "id_urzadzenia": 3,
        "nazwa_modelu": "WindFree Avant",
        "liczba_alertow_urzadzenia": 3
      }
    }'::jsonb
);

-- Raport 3: Porównanie prognozy z rzeczywistością dla "Czujnika Siemens" (ID=1)
INSERT INTO Raporty (ID_uzytkownika, czas_wygenerowania, typ_raportu, opis, zakres_od, zakres_do, zawartosc)
VALUES
(
    2, -- Wygenerowany przez Inżyniera (ID=2)
    '2025-11-25 11:00:00',
    'ocena_dokladnosci_prognozy',
    'Analiza dokładności prognoz temperatury dla czujnika w pokoju 101',
    '2025-11-24 00:00:00',
    '2025-11-24 23:59:59',
    '{
      "id_urzadzenia": 1,
      "metryka": "temperatura_C",
      "ocena_modelu": {
        "sredni_blad_procentowy_mape": 2.5,
        "maksymalne_odchylenie_C": 1.8,
        "komentarz": "Model działa z zadowalającą dokładnością."
      }
    }'::jsonb
);