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