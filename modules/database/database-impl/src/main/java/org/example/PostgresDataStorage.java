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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    public List<Urzadzenie> getActiveDevices() {
        // Proste zapytanie, które pobiera wszystkie aktywne urządzenia.
        String sql = "SELECT * FROM Urzadzenia WHERE aktywny = true";

        List<Urzadzenie> activeDevices = new ArrayList<>();

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Urzadzenie device = new Urzadzenie();

                // Mapujemy dane bezpośrednio na nasz istniejący obiekt DTO Urzadzenie
                device.setId(rs.getInt("ID_urzadzenia"));
                device.setPokojId(rs.getInt("ID_pokoju"));
                device.setModelId(rs.getInt("ID_modelu"));
                device.setParametryPracy(rs.getString("Parametry_pracy"));
                device.setAktywny(rs.getBoolean("aktywny"));
                // (Jeśli w DTO Urzadzenie masz pole 'aktywny', też je ustaw)
                // device.setAktywny(rs.getBoolean("aktywny"));

                activeDevices.add(device);
            }
        } catch (SQLException e) {
            System.out.println("Błąd podczas pobierania aktywnych urządzeń: " + e.getMessage());
            e.printStackTrace();
        }

        return activeDevices;
    }

    @Override
    public List<UrzadzenieSzczegoly> getActiveDevicesWithDetails() {
        // SQL pozostaje bez zmian - pobieramy całą kolumnę JSON
        String sql = "SELECT u.*, p.numer_pokoju, m.nazwa_modelu, tu.nazwa_typu_urzadzenia, pu.nazwa_producenta " +
                "FROM Urzadzenia u " +
                "JOIN Pokoje p ON u.ID_pokoju = p.ID_pokoju " +
                "JOIN Model_urzadzenia m ON u.ID_modelu = m.ID_modelu " +
                "JOIN Typ_urzadzenia tu ON m.ID_typu_urzadzenia = tu.ID_typu_urzadzenia " +
                "JOIN Producent_urzadzenia pu ON m.ID_producenta = pu.ID_producenta " +
                "WHERE u.aktywny = true";

        List<UrzadzenieSzczegoly> devicesWithDetails = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                UrzadzenieSzczegoly device = new UrzadzenieSzczegoly();

                // Mapowanie standardowe
                device.setId(rs.getInt("ID_urzadzenia"));
                device.setPokojId(rs.getInt("ID_pokoju"));
                device.setModelId(rs.getInt("ID_modelu"));
                device.setParametryPracy(rs.getString("Parametry_pracy"));
                device.setAktywny(rs.getBoolean("aktywny"));
                device.setNazwaPokoju(rs.getString("numer_pokoju"));
                device.setNazwaModelu(rs.getString("nazwa_modelu"));
                device.setNazwaTypu(rs.getString("nazwa_typu_urzadzenia"));
                device.setNazwaProducenta(rs.getString("nazwa_producenta"));

                // Parsowanie JSON
                String paramsJson = rs.getString("Parametry_pracy");
                if (paramsJson != null && !paramsJson.isEmpty()) {
                    try {
                        JsonNode rootNode = objectMapper.readTree(paramsJson);

                        // ==========================================
                        // 1. MOC (Dla każdego urządzenia)
                        // ==========================================
                        // Używamy .path() zamiast .get() - to bezpieczniejsze.
                        // Jeśli "moc_W" nie istnieje, .asInt(0) wstawi 0.
                        device.setMocW(rootNode.path("moc_W").asInt(0));

                        // ==========================================
                        // 2. Parametry specyficzne (Oświetlenie)
                        // ==========================================
                        if (rootNode.has("sciemnialna")) {
                            device.setSciemnialna(rootNode.get("sciemnialna").asBoolean());
                        }
                        if (rootNode.has("barwa_K")) {
                            device.setBarwaK(rootNode.get("barwa_K").asInt());
                        }

                        // ==========================================
                        // 3. Parametry specyficzne (Czujniki)
                        // ==========================================
                        if (rootNode.has("etykieta_metryki")) {
                            device.setMetricLabel(rootNode.get("etykieta_metryki").asText());
                        }

                        JsonNode zakresNode = rootNode.path("zakres_pomiaru");
                        if (!zakresNode.isMissingNode()) {
                            if (zakresNode.has("min")) {
                                device.setMinRange(zakresNode.get("min").asDouble());
                            }
                            if (zakresNode.has("max")) {
                                device.setMaxRange(zakresNode.get("max").asDouble());
                            }
                        }

                    } catch (Exception e) {
                        System.err.println("Błąd JSON ID " + device.getId() + ": " + e.getMessage());
                    }
                } else {
                    // Jeśli JSON jest pusty/null, ustawiamy moc na 0
                    device.setMocW(0);
                }

                devicesWithDetails.add(device);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return devicesWithDetails;
    }

    @Override
    public boolean isDatabaseConnected() {
        String sql = "SELECT 1";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeQuery();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public void saveAlert(Alert alert) {
        String sql = "INSERT INTO Alerty (ID_urzadzenia, priorytet, status, tresc, czas_alertu) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, alert.getUrzadzenieId());
            // Konwertujemy enum na String przed zapisem do bazy
            pstmt.setString(2, alert.getPriorytet().name());
            pstmt.setString(3, alert.getStatus().name());
            pstmt.setString(4, alert.getTresc());
            pstmt.setTimestamp(5, Timestamp.valueOf(alert.getCzasAlertu()));
            pstmt.executeUpdate();
        } catch (SQLException e) {
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

            while (rs.next()) {
                // Używamy naszej nowej, centralnej metody do mapowania.
                // Cała logika mapowania jest teraz w jednym miejscu.
                alerts.add(mapResultSetToAlert(rs));
            }

        } catch (SQLException e) {
            System.out.println("Błąd podczas pobierania alertów dla urządzenia: " + e.getMessage());
            e.printStackTrace();
        }

        return alerts;
    }

    @Override
    public void updateAlertStatus(int alertId, Alert.AlertStatus newStatus) {
        String sql = "UPDATE Alerty SET status = ? WHERE ID_alertu = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Konwertujemy enum na String
            pstmt.setString(1, newStatus.name());
            pstmt.setInt(2, alertId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Alert> getAlertsForBuilding(int buildingId) {
        String sql = "SELECT a.* FROM Alerty a " +
                "JOIN Urzadzenia u ON a.ID_urzadzenia = u.ID_urzadzenia " +
                "JOIN Pokoje p ON u.ID_pokoju = p.ID_pokoju " +
                "WHERE p.ID_budynku = ? ORDER BY a.czas_alertu DESC";
        List<Alert> alerts = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, buildingId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                alerts.add(mapResultSetToAlert(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return alerts;
    }

    @Override
    public List<Alert> getActiveAlertsBySeverity(Alert.AlertSeverity severity) {
        String sql = "SELECT * FROM Alerty WHERE priorytet = ? AND status != 'ROZWIAZANY' ORDER BY czas_alertu DESC";
        List<Alert> alerts = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, severity.name());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                alerts.add(mapResultSetToAlert(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return alerts;
    }

    @Override
    public List<AlertSzczegoly> getAlertDetailsForBuilding(int buildingId) {
        return getAlertDetailsWithFilter("WHERE p.ID_budynku = ?", buildingId);
    }

    @Override
    public List<AlertSzczegoly> getAlertDetailsForRoom(int roomId) {
        return getAlertDetailsWithFilter("WHERE p.ID_pokoju = ?", roomId);
    }

    @Override
    public AlertSzczegoly getAlertDetailsById(int alertId) {
        List<AlertSzczegoly> result = getAlertDetailsWithFilter("WHERE a.ID_alertu = ?", alertId);
        // Zwróć pierwszy element z listy lub null, jeśli lista jest pusta
        return result.isEmpty() ? null : result.get(0);
    }

    private Alert mapResultSetToAlert(ResultSet rs) throws SQLException {
        Alert alert = new Alert();
        alert.setId(rs.getInt("ID_alertu"));
        alert.setUrzadzenieId(rs.getInt("ID_urzadzenia"));
        alert.setTresc(rs.getString("tresc"));
        if (rs.getTimestamp("czas_alertu") != null) {
            alert.setCzasAlertu(rs.getTimestamp("czas_alertu").toLocalDateTime());
        }
        // Konwertujemy String z bazy na Enum w Javie
        alert.setPriorytet(Alert.AlertSeverity.valueOf(rs.getString("priorytet")));
        alert.setStatus(Alert.AlertStatus.valueOf(rs.getString("status")));
        return alert;
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
    public Uzytkownik getUserById(int userId) {
        String sql = "SELECT * FROM Uzytkownik WHERE ID_uzytkownika = ?";
        Uzytkownik user = null;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                user = new Uzytkownik();
                user.setId(rs.getInt("ID_uzytkownika"));
                user.setRolaId(rs.getInt("ID_roli"));
                user.setImie(rs.getString("Imie"));
                user.setNazwisko(rs.getString("Nazwisko"));
                user.setTelefon(rs.getString("Telefon"));
                user.setEmail(rs.getString("Email"));
                user.setHasloHash(rs.getString("Haslo_hash"));
                user.setPreferencje(rs.getString("preferencje"));
            }
        } catch (SQLException e) {
            System.out.println("Błąd podczas pobierania użytkownika o ID " + userId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return user;
    }

    @Override
    public Umowa getActiveContractForBuilding(int buildingId) {
        // Zapytanie, które znajduje aktywną umowę (data końca jest w przyszłości lub jest pusta/NULL)
        String sql = "SELECT * FROM Umowa WHERE ID_budynku = ? AND (data_konca IS NULL OR data_konca > CURRENT_DATE) ORDER BY data_poczatku DESC LIMIT 1";
        Umowa contract = null;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, buildingId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                contract = new Umowa();
                contract.setId(rs.getInt("ID_umowy"));
                contract.setBudynekId(rs.getInt("ID_budynku"));
                contract.setDostawcaId(rs.getInt("ID_dostawcy"));

                // Konwersja z java.sql.Date na java.time.LocalDate
                Date dataPoczatkuSql = rs.getDate("data_poczatku");
                if (dataPoczatkuSql != null) {
                    contract.setDataPoczatku(dataPoczatkuSql.toLocalDate());
                }

                Date dataKoncaSql = rs.getDate("data_konca");
                if (dataKoncaSql != null) {
                    contract.setDataKonca(dataKoncaSql.toLocalDate());
                }

                contract.setSzczegolyTaryfy(rs.getString("szczegoly_taryfy"));
            }
        } catch (SQLException e) {
            System.out.println("Błąd podczas pobierania aktywnej umowy dla budynku o ID " + buildingId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return contract;
    }

    @Override
    public List<Odczyt> getReadingsForBuilding(int buildingId, LocalDateTime from, LocalDateTime to) {
        List<Integer> deviceIdsInBuilding = new ArrayList<>();
        String sql = "SELECT u.id_urzadzenia FROM Urzadzenia u JOIN Pokoje p ON u.id_pokoju = p.id_pokoju WHERE p.id_budynku = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, buildingId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                deviceIdsInBuilding.add(rs.getInt("id_urzadzenia"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
        if (deviceIdsInBuilding.isEmpty()) {
            return new ArrayList<>();
        }
        List<Odczyt> readings = new ArrayList<>();
        try (MongoClient mongoClient = MongoClients.create(MONGO_URI)) {
            MongoDatabase database = mongoClient.getDatabase(MONGO_DATABASE);
            MongoCollection<Document> collection = database.getCollection(MONGO_COLLECTION);
            Bson filter = Filters.and(
                    Filters.in("id_urzadzenia", deviceIdsInBuilding),
                    Filters.gte("czas_odczytu", from.toInstant(ZoneOffset.UTC)),
                    Filters.lt("czas_odczytu", to.toInstant(ZoneOffset.UTC))
            );
            FindIterable<Document> documents = collection.find(filter);
            for (Document doc : documents) {
                Odczyt reading = new Odczyt();
                reading.setUrzadzenieId(doc.getInteger("id_urzadzenia"));
                reading.setCzasOdczytu(doc.getDate("czas_odczytu").toInstant());
                if (doc.get("pomiary") != null) {
                    reading.setPomiary(doc.get("pomiary", Document.class).toJson());
                }
                readings.add(reading);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return readings;
    }

    @Override
    public List<AlertSzczegoly> getAlertDetailsForBuilding(int buildingId, LocalDateTime from, LocalDateTime to) {
        String sql = "SELECT a.*, u.nazwa_wlasna, m.nazwa_modelu, p.numer_pokoju, b.Nazwa AS nazwa_budynku " +
                "FROM Alerty a " +
                "JOIN Urzadzenia u ON a.ID_urzadzenia = u.ID_urzadzenia " +
                "JOIN Pokoje p ON u.ID_pokoju = p.ID_pokoju " +
                "JOIN Budynek b ON p.ID_budynku = b.ID_budynku " +
                "JOIN Model_urzadzenia m ON u.ID_modelu = m.ID_modelu " +
                "WHERE p.ID_budynku = ? AND a.czas_alertu >= ? AND a.czas_alertu < ? ORDER BY a.czas_alertu DESC";

        List<AlertSzczegoly> alertDetails = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, buildingId);
            pstmt.setTimestamp(2, Timestamp.valueOf(from));
            pstmt.setTimestamp(3, Timestamp.valueOf(to));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                alertDetails.add(mapResultSetToAlertDetails(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return alertDetails;
    }


    @Override
    public List<Prognoza> getForecastsForDevice(int deviceId, LocalDateTime from, LocalDateTime to) {
        String sql = "SELECT * FROM Prognozy WHERE ID_urzadzenia = ? AND czas_prognozy >= ? AND czas_prognozy < ? ORDER BY czas_prognozy";
        List<Prognoza> forecasts = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, deviceId);
            pstmt.setTimestamp(2, Timestamp.valueOf(from));
            pstmt.setTimestamp(3, Timestamp.valueOf(to));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                forecasts.add(mapResultSetToPrognoza(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return forecasts;
    }


    @Override
    public List<Prognoza> getForecastsForBuilding(int buildingId, LocalDateTime from, LocalDateTime to) {
        String sql = "SELECT * FROM Prognozy WHERE ID_budynku = ? AND czas_prognozy >= ? AND czas_prognozy < ? ORDER BY czas_prognozy";
        List<Prognoza> forecasts = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, buildingId);
            pstmt.setTimestamp(2, Timestamp.valueOf(from));
            pstmt.setTimestamp(3, Timestamp.valueOf(to));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                forecasts.add(mapResultSetToPrognoza(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return forecasts;
    }

    @Override
    public void storeForecastResult(Prognoza forecast) {
        String sql = "INSERT INTO Prognozy (ID_urzadzenia, ID_budynku, czas_wygenerowania, czas_prognozy, prognozowana_wartosc, metryka) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Używamy setObject, aby poprawnie obsłużyć wartości null
            pstmt.setObject(1, forecast.getUrzadzenieId());
            pstmt.setObject(2, forecast.getBudynekId());

            pstmt.setTimestamp(3, Timestamp.valueOf(forecast.getCzasWygenerowania()));
            pstmt.setTimestamp(4, Timestamp.valueOf(forecast.getCzasPrognozy()));
            pstmt.setDouble(5, forecast.getPrognozowanaWartosc());
            pstmt.setString(6, forecast.getMetryka());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Błąd podczas zapisywania wyniku prognozy: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public double getTotalEnergyConsumptionForBuilding(int buildingId, LocalDateTime from, LocalDateTime to) {
        List<Odczyt> readings = getReadingsForBuilding(buildingId, from, to);
        double totalConsumption = 0.0;
        for (Odczyt reading : readings) {
            try {
                Document pomiaryDoc = Document.parse(reading.getPomiary());
                if (pomiaryDoc.containsKey("zuzycie_kwh")) {
                    Object value = pomiaryDoc.get("zuzycie_kwh");
                    if (value instanceof Number) {
                        totalConsumption += ((Number) value).doubleValue();
                    }
                }
            } catch (Exception e) {
                // Ignoruj błędy
            }
        }
        return totalConsumption;
    }

    @Override
    public Raport saveReport(Raport report) {
        String sql = "INSERT INTO Raporty (ID_uzytkownika, czas_wygenerowania, typ_raportu, opis, zakres_od, zakres_do, zawartosc) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setObject(1, report.getUzytkownikId());
            pstmt.setTimestamp(2, Timestamp.valueOf(report.getCzasWygenerowania()));
            pstmt.setString(3, report.getTypRaportu());
            pstmt.setString(4, report.getOpis());
            pstmt.setTimestamp(5, Timestamp.valueOf(report.getZakresOd()));
            pstmt.setTimestamp(6, Timestamp.valueOf(report.getZakresDo()));
            pstmt.setString(7, report.getZawartosc());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        report.setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return report;
    }

    @Override
    public List<Raport> getReportsByType(String reportType) {
        String sql = "SELECT * FROM Raporty WHERE typ_raportu = ? ORDER BY czas_wygenerowania DESC";
        List<Raport> reports = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, reportType);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                reports.add(mapResultSetToRaport(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reports;
    }

    @Override
    public Raport getReportById(int reportId) {
        String sql = "SELECT * FROM Raporty WHERE ID_raportu = ?";
        Raport report = null;
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, reportId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                report = mapResultSetToRaport(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return report;
    }

    // PRYWATNA METODA POMOCNICZA DO MAPOWANIA
    private Raport mapResultSetToRaport(ResultSet rs) throws SQLException {
        Raport report = new Raport();
        report.setId(rs.getInt("ID_raportu"));
        report.setUzytkownikId((Integer) rs.getObject("ID_uzytkownika"));
        report.setCzasWygenerowania(rs.getTimestamp("czas_wygenerowania").toLocalDateTime());
        report.setTypRaportu(rs.getString("typ_raportu"));
        report.setOpis(rs.getString("opis"));
        report.setZakresOd(rs.getTimestamp("zakres_od").toLocalDateTime());
        report.setZakresDo(rs.getTimestamp("zakres_do").toLocalDateTime());
        report.setZawartosc(rs.getString("zawartosc"));
        return report;
    }

    private Prognoza mapResultSetToPrognoza(ResultSet rs) throws SQLException {
        Prognoza forecast = new Prognoza();
        forecast.setId(rs.getInt("ID_prognozy"));
        forecast.setUrzadzenieId((Integer) rs.getObject("ID_urzadzenia"));
        forecast.setBudynekId((Integer) rs.getObject("ID_budynku"));
        forecast.setCzasWygenerowania(rs.getTimestamp("czas_wygenerowania").toLocalDateTime());
        forecast.setCzasPrognozy(rs.getTimestamp("czas_prognozy").toLocalDateTime());
        forecast.setPrognozowanaWartosc(rs.getDouble("prognozowana_wartosc"));
        forecast.setMetryka(rs.getString("metryka"));
        return forecast;
    }



    @Override
    public Urzadzenie getDeviceById(int deviceId) {
        String sql = "SELECT * FROM Urzadzenia WHERE ID_urzadzenia = ?";
        Urzadzenie device = null;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, deviceId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                device = new Urzadzenie();
                device.setId(rs.getInt("ID_urzadzenia"));
                device.setPokojId(rs.getInt("ID_pokoju"));
                device.setModelId(rs.getInt("ID_modelu"));
                device.setParametryPracy(rs.getString("Parametry_pracy"));
            }

        } catch (SQLException e) {
            System.out.println("Błąd podczas pobierania urządzenia o ID " + deviceId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return device;
    }

    @Override
    public List<Urzadzenie> getDevicesInRoom(int roomId) {
        String sql = "SELECT * FROM Urzadzenia WHERE ID_pokoju = ?";
        List<Urzadzenie> devices = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, roomId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Urzadzenie device = new Urzadzenie();
                device.setId(rs.getInt("ID_urzadzenia"));
                device.setPokojId(rs.getInt("ID_pokoju"));
                device.setModelId(rs.getInt("ID_modelu"));
                device.setParametryPracy(rs.getString("Parametry_pracy"));

                devices.add(device);
            }

        } catch (SQLException e) {
            System.out.println("Błąd podczas pobierania urządzeń dla pokoju o ID " + roomId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return devices;
    }

    @Override
    public List<Pokoj> getRoomsInBuilding(int buildingId) {
        String sql = "SELECT * FROM Pokoje WHERE ID_budynku = ?";
        List<Pokoj> rooms = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, buildingId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Pokoj room = new Pokoj();
                room.setId(rs.getInt("ID_pokoju"));
                room.setBudynekId(rs.getInt("ID_budynku"));
                room.setNumerPokoju(rs.getString("numer_pokoju"));
                room.setPietro(rs.getInt("pietro"));

                rooms.add(room);
            }
        } catch (SQLException e) {
            System.out.println("Błąd podczas pobierania pokoi dla budynku o ID " + buildingId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return rooms;
    }

    @Override
    public void updateDevice(Urzadzenie device) {
        // Nowe zapytanie SQL, które aktualizuje wiele kolumn.
        String sql = "UPDATE Urzadzenia SET ID_pokoju = ?, Parametry_pracy = ?::jsonb, aktywny = ? WHERE ID_urzadzenia = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Ustawiamy wartości dla wszystkich placeholderów (?)
            pstmt.setInt(1, device.getPokojId());
            pstmt.setString(2, device.getParametryPracy());
            pstmt.setBoolean(3, device.isAktywny());

            // Ostatni placeholder to ID w klauzuli WHERE
            pstmt.setInt(4, device.getId());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Pomyślnie zaktualizowano urządzenie o ID: " + device.getId());
            } else {
                System.out.println("OSTRZEŻENIE: Nie zaktualizowano żadnego urządzenia. Sprawdź, czy urządzenie o ID " + device.getId() + " istnieje.");
            }

        } catch (SQLException e) {
            System.out.println("Błąd podczas aktualizacji urządzenia: " + e.getMessage());
            e.printStackTrace();
        }
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
        String sql = "SELECT * FROM Rola WHERE ID_roli = ?";
        Rola rola = null;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, rolaId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                rola = new Rola();
                rola.setId(rs.getInt("ID_roli"));
                rola.setNazwaRoli(rs.getString("Nazwa_roli"));
                rola.setOpisRoli(rs.getString("Opis_roli"));
            }

        } catch (SQLException e) {
            System.out.println("Błąd podczas pobierania roli o ID " + rolaId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return rola;
    }

    @Override
    public Uzytkownik saveUser(Uzytkownik user) {
        // Zapytanie do wstawienia nowego użytkownika.
        // Statement.RETURN_GENERATED_KEYS poprosi bazę o zwrot wygenerowanego ID.
        String sql = "INSERT INTO Uzytkownik (ID_roli, Imie, Nazwisko, Telefon, Email, Haslo_hash, preferencje) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, user.getRolaId());
            pstmt.setString(2, user.getImie());
            pstmt.setString(3, user.getNazwisko());
            pstmt.setString(4, user.getTelefon());
            pstmt.setString(5, user.getEmail());
            pstmt.setString(6, user.getHasloHash());
            pstmt.setString(7, user.getPreferencje());

            int affectedRows = pstmt.executeUpdate();

            // Jeśli wiersz został dodany, pobieramy wygenerowane ID
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        // Ustawiamy ID w obiekcie, który zwrócimy
                        user.setId(generatedKeys.getInt(1));
                    } else {
                        throw new SQLException("Tworzenie użytkownika nie powiodło się, brak wygenerowanego ID.");
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Błąd podczas zapisywania użytkownika: " + e.getMessage());
            e.printStackTrace();
            return null; // Zwracamy null w razie błędu
        }
        // Zwracamy obiekt użytkownika z nowym ID
        return user;
    }

    private List<AlertSzczegoly> getAlertDetailsWithFilter(String whereClause, Object... params) {
        String baseSql = "SELECT a.*, u.nazwa_wlasna, m.nazwa_modelu, p.numer_pokoju, b.Nazwa AS nazwa_budynku " +
                "FROM Alerty a " +
                "JOIN Urzadzenia u ON a.ID_urzadzenia = u.ID_urzadzenia " +
                "JOIN Pokoje p ON u.ID_pokoju = p.ID_pokoju " +
                "JOIN Budynek b ON p.ID_budynku = b.ID_budynku " +
                "JOIN Model_urzadzenia m ON u.ID_modelu = m.ID_modelu ";

        String finalSql = baseSql + whereClause + " ORDER BY a.czas_alertu DESC";

        List<AlertSzczegoly> alertDetails = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(finalSql)) {

            // Ustawiamy parametry dla klauzuli WHERE
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                // Używamy naszej istniejącej metody pomocniczej do mapowania
                // (Musimy ją lekko zmodyfikować, aby zwracała AlertDetails)
                alertDetails.add(mapResultSetToAlertDetails(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return alertDetails;
    }

    // Zaktualizuj lub stwórz tę metodę pomocniczą do mapowania
    private AlertSzczegoly mapResultSetToAlertDetails(ResultSet rs) throws SQLException {
        AlertSzczegoly details = new AlertSzczegoly();
        // Mapowanie pól z klasy bazowej Alert
        details.setId(rs.getInt("ID_alertu"));
        details.setUrzadzenieId(rs.getInt("ID_urzadzenia"));
        details.setTresc(rs.getString("tresc"));
        if (rs.getTimestamp("czas_alertu") != null) {
            details.setCzasAlertu(rs.getTimestamp("czas_alertu").toLocalDateTime());
        }
        details.setPriorytet(Alert.AlertSeverity.valueOf(rs.getString("priorytet")));
        details.setStatus(Alert.AlertStatus.valueOf(rs.getString("status")));
        // Mapowanie nowych, dodatkowych pól
        details.setNazwaUrzadzenia(rs.getString("nazwa_wlasna"));
        details.setNazwaModelu(rs.getString("nazwa_modelu"));
        details.setNazwaPokoju(rs.getString("numer_pokoju"));
        details.setNazwaBudynku(rs.getString("nazwa_budynku"));
        return details;
    }
}
