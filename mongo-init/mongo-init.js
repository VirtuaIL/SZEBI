// =============================================================================
// == SKRYPT INICJALIZACYJNY DLA BAZY DANYCH MONGODB
// =============================================================================
// Ten skrypt zostanie automatycznie wykonany przez kontener MongoDB przy pierwszym uruchomieniu.

print(">>>> Rozpoczęcie inicjalizacji bazy danych MongoDB...");

// Wybór bazy danych. Jeśli baza o nazwie 'szebi_timeseries_db' nie istnieje,
// MongoDB przygotuje się do jej stworzenia przy pierwszej operacji zapisu.
db = db.getSiblingDB('szebi_timeseries_db');

// Stworzenie kolekcji 'odczyty_urzadzen'. Dobrą praktyką jest tworzenie jej
// jawnie, zamiast polegać na automatycznym tworzeniu przy pierwszym zapisie.
db.createCollection('odczyty_urzadzen');

// Przygotowanie tablicy z przykładowymi dokumentami (odczytami).
// ID urządzeń (1, 2, 3) odpowiadają tym, które stworzyliśmy w postgres/init.sql
var odczyty = [
    // --- Odczyty dla Czujnika Temperatury (ID_urzadzenia = 1) ---
    {
        "czas_odczytu": new ISODate("2025-12-01T12:00:00Z"),
        "id_urzadzenia": 1,
        "pomiary": { "temperatura_C": 21.5, "zuzycie_energii_W": 0.1 }
    },
    {
        "czas_odczytu": new ISODate("2025-12-01T12:01:00Z"),
        "id_urzadzenia": 1,
        "pomiary": { "temperatura_C": 21.6, "zuzycie_energii_W": 0.1 }
    },
    
    // --- Odczyty dla Czujnika Wilgotności (ID_urzadzenia = 2) ---
    {
        "czas_odczytu": new ISODate("2025-12-01T12:00:00Z"),
        "id_urzadzenia": 2,
        "pomiary": { "wilgotnosc_procent": 45, "zuzycie_energii_W": 0.2 }
    },
    {
        "czas_odczytu": new ISODate("2025-12-01T12:01:00Z"),
        "id_urzadzenia": 2,
        "pomiary": { "wilgotnosc_procent": 45.5, "zuzycie_energii_W": 0.2 }
    },

    // --- Odczyty dla Lampy Philips Hue (ID_urzadzenia = 3) ---
    {
        "czas_odczytu": new ISODate("2025-12-01T12:00:00Z"),
        "id_urzadzenia": 3,
        "pomiary": { "stan": "on", "jasnosc_procent": 100, "zuzycie_energii_W": 9.0 }
    },
    {
        "czas_odczytu": new ISODate("2025-12-01T12:01:00Z"),
        "id_urzadzenia": 3,
        "pomiary": { "stan": "on", "jasnosc_procent": 80, "zuzycie_energii_W": 7.2 }
    }
];

// Wstawienie wszystkich przygotowanych odczytów do kolekcji 'odczyty_urzadzen'.
db.odczyty_urzadzen.insertMany(odczyty);

print(">>>> Baza MongoDB zainicjalizowana pomyślnie! Dodano " + odczyty.length + " przykładowych odczytów. <<<<");