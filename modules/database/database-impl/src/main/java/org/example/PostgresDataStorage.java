package org.example;

import org.example.DTO.*;
import org.example.interfaces.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import org.bson.conversions.Bson;
import java.time.ZoneOffset;

public class PostgresDataStorage implements IUserData, IAcquisitionData, IAlertData, IControlData, IAnalyticsData, IForecastingData{

    //config postgres
    private final String DB_URL = "jdbc:postgresql://localhost:5433/szebi_db_nowa";
    private final String USER = "admin";
    private final String PASS = "bazka_haslo";

    //config mongo
    private final String MONGO_URI = "mongodb://root:bazka@localhost:27018/";
    private final String MONGO_DATABASE = "szebi_timeseries_db";
    private final String MONGO_COLLECTION = "odczyty_urzadzen";



    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }


    @Override
    public void saveSensorReading(Odczyt reading) {
        // Używamy try-with-resources, aby automatycznie zamknąć połączenie z MongoDB
        try (MongoClient mongoClient = MongoClients.create(MONGO_URI)) {

            // Krok 1: Wybierz bazę danych
            MongoDatabase database = mongoClient.getDatabase(MONGO_DATABASE);

            // Krok 2: Wybierz kolekcję
            MongoCollection<Document> collection = database.getCollection(MONGO_COLLECTION);

            // Krok 3: Użyj metody toDocument() z naszego DTO, aby przygotować dane
            Document docToInsert = reading.toDocument();

            // Krok 4: Wstaw dokument do kolekcji
            collection.insertOne(docToInsert);

            // System.out.println("Zapisano odczyt dla urządzenia " + reading.getUrzadzenieId() + " w MongoDB.");

        } catch (Exception e) {
            System.out.println("Błąd podczas zapisywania odczytu do MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void saveBatchSensorReadings(List<Odczyt> readings) {
        // Sprawdzamy, czy lista odczytów nie jest pusta, aby uniknąć niepotrzebnego łączenia z bazą.
        if (readings == null || readings.isEmpty()) {
            System.out.println("Lista odczytów jest pusta, nic do zapisania.");
            return;
        }

        try (MongoClient mongoClient = MongoClients.create(MONGO_URI)) {
            MongoDatabase database = mongoClient.getDatabase(MONGO_DATABASE);
            MongoCollection<Document> collection = database.getCollection(MONGO_COLLECTION);

            // Krok 1: Przygotuj listę dokumentów BSON do wstawienia.
            List<Document> documentsToInsert = new ArrayList<>();
            for (Odczyt reading : readings) {
                documentsToInsert.add(reading.toDocument());
            }

            // Krok 2: Użyj metody insertMany, aby wstawić wszystkie dokumenty w jednej operacji.
            collection.insertMany(documentsToInsert);

            // System.out.println("Zapisano " + readings.size() + " odczytów w MongoDB.");

        } catch (Exception e) {
            System.out.println("Błąd podczas hurtowego zapisywania odczytów do MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void saveAlert(Alert alert) {
        // Krok 1: Przygotuj zapytanie SQL z placeholderami (?)
        String sql = "INSERT INTO Alerty (ID_urzadzenia, tresc, czas_alertu) VALUES (?, ?, ?)";

        // Krok 2: Użyj try-with-resources do zarządzania połączeniem
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Krok 3: Ustaw wartości dla placeholderów
            pstmt.setInt(1, alert.getUrzadzenieId());
            pstmt.setString(2, alert.getTresc());
            pstmt.setTimestamp(3, java.sql.Timestamp.valueOf(alert.getCzasAlertu()));

            // Krok 4: Wykonaj zapytanie
            pstmt.executeUpdate();

        } catch (SQLException e) {
            // Podstawowa obsługa błędów
            System.out.println("Błąd podczas zapisywania alertu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<Alert> getAlertsByDeviceId(int deviceId) {
        String sql = "SELECT * FROM Alerty WHERE ID_urzadzenia = ? ORDER BY czas_alertu DESC";
        List<Alert> alerts = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, deviceId);
            ResultSet rs = pstmt.executeQuery();

            // Pętla `while` przejdzie przez wszystkie znalezione alerty
            while (rs.next()) {
                Alert alert = new Alert();

                // Mapujemy dane z wiersza bazy na obiekt Alert
                alert.setId(rs.getInt("ID_alertu"));
                alert.setUrzadzenieId(rs.getInt("ID_urzadzenia"));
                alert.setTresc(rs.getString("tresc"));
                // Konwersja Timestamp z bazy na LocalDateTime w Javie
                if (rs.getTimestamp("czas_alertu") != null) {
                    alert.setCzasAlertu(rs.getTimestamp("czas_alertu").toLocalDateTime());
                }
                // Możesz też dodać obsługę statusu, jeśli masz tę kolumnę
                // alert.setStatus(rs.getString("status"));

                alerts.add(alert); // Dodajemy zmapowany obiekt do listy
            }

        } catch (SQLException e) {
            System.out.println("Błąd podczas pobierania alertów dla urządzenia: " + e.getMessage());
            e.printStackTrace();
        }

        // Zwracamy listę (będzie pusta, jeśli urządzenie nie ma alertów)
        return alerts;
    }

    @Override
    public List<Odczyt> getReadingsForDevice(int deviceId, LocalDateTime from, LocalDateTime to) {
        List<Odczyt> readings = new ArrayList<>();

        try (MongoClient mongoClient = MongoClients.create(MONGO_URI)) {
            MongoDatabase database = mongoClient.getDatabase(MONGO_DATABASE);
            MongoCollection<Document> collection = database.getCollection(MONGO_COLLECTION);

            // Krok 1: Zbuduj filtr zapytania dla MongoDB
            Bson filter = Filters.and(
                    Filters.eq("id_urzadzenia", deviceId),
                    Filters.gte("czas_odczytu", from.toInstant(ZoneOffset.UTC)), // gte = greater than or equal
                    Filters.lt("czas_odczytu", to.toInstant(ZoneOffset.UTC))      // lt = less than
            );

            // Krok 2: Wykonaj zapytanie find() z naszym filtrem
            FindIterable<Document> documents = collection.find(filter);

            // Krok 3: Przejdź przez wszystkie znalezione dokumenty i zmapuj je na obiekty Odczyt
            for (Document doc : documents) {
                Odczyt reading = new Odczyt();

                // Mapujemy dane z dokumentu BSON na nasz obiekt DTO
                reading.setUrzadzenieId(doc.getInteger("id_urzadzenia"));
                // Konwertujemy datę z formatu MongoDB na Instant w Javie
                reading.setCzasOdczytu(doc.getDate("czas_odczytu").toInstant());

                // Konwertujemy zagnieżdżony dokument 'pomiary' z powrotem na String JSON
                if (doc.get("pomiary") != null) {
                    reading.setPomiary(doc.get("pomiary", Document.class).toJson());
                }

                readings.add(reading);
            }

        } catch (Exception e) {
            System.out.println("Błąd podczas pobierania odczytów z MongoDB: " + e.getMessage());
            e.printStackTrace();
        }

        // Zwracamy listę odczytów (może być pusta)
        return readings;
    }

    @Override
    public List<Odczyt> getReadingsForBuilding(int buildingId, LocalDateTime from, LocalDateTime to) {
        return List.of();
    }

    @Override
    public double getTotalEnergyConsumptionForBuilding(int buildingId, LocalDateTime from, LocalDateTime to) {
        return 0;
    }

    @Override
    public Urzadzenie getDeviceById(int deviceId) {
        return null;
    }

    @Override
    public List<Urzadzenie> getDevicesInRoom(int roomId) {
        return List.of();
    }

    @Override
    public void updateDevice(Urzadzenie device) {

    }

    @Override
    public Uzytkownik getUserByEmail(String email) {
        String sql = "SELECT * FROM Uzytkownik WHERE Email = ?";
        Uzytkownik user = null;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            // Sprawdzamy, czy znaleziono jakikolwiek rekord
            if (rs.next()) {
                // Jeśli tak, tworzymy nowy obiekt Uzytkownik i mapujemy dane
                user = new Uzytkownik();
                user.setId(rs.getInt("ID_uzytkownika"));
                user.setRolaId(rs.getInt("ID_roli"));
                user.setImie(rs.getString("Imie"));
                user.setNazwisko(rs.getString("Nazwisko"));
                user.setTelefon(rs.getString("Telefon"));
                user.setEmail(rs.getString("Email"));
                user.setHasloHash(rs.getString("Haslo_hash"));
                // (Możesz dodać obsługę reszty pól, jak preferencje)
            }

        } catch (SQLException e) {
            System.out.println("Błąd podczas pobierania użytkownika: " + e.getMessage());
            e.printStackTrace();
        }
        // Zwracamy obiekt użytkownika lub null, jeśli nie znaleziono
        return user;
    }

    @Override
    public Rola getRoleById(int rolaId) {
        return null;
    }

    @Override
    public Uzytkownik saveUser(Uzytkownik user) {
        return null;
    }

    @Override
    public List<Budynek> getBuildingsForUser(int userId) {
        return List.of();
    }
}
