package org.example;

import org.example.DTO.Prognoza;
import org.example.interfaces.IForecastingData;
import org.example.interfaces.IAnalyticsData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.time.LocalDateTime;
import java.util.List;

public class TestForecastModule {
    
    public static void main(String[] args) {
        System.out.println("=== TEST MODUŁU PROGNOZOWANIA ===\n");
        
        try {
            IForecastingData database = new PostgresDataStorage();
            IAnalyticsData analytics = (IAnalyticsData) database;
            
            ForecastServiceAPI service = new ForecastServiceAPI(database, analytics);
            ObjectMapper mapper = new ObjectMapper();
            
            int deviceId = 1;
            
            System.out.println("1. Generowanie prognozy dla urządzenia " + deviceId + "...");
            try {
                service.generateForecast(deviceId);
                System.out.println("   ✓ Prognoza wygenerowana pomyślnie\n");
            } catch (IncompleteDataException e) {
                System.out.println("   ✗ Błąd: " + e.getMessage());
                System.out.println("   (Upewnij się, że masz dane testowe w bazie)\n");
                return;
            }
            
            System.out.println("2. Pobieranie prognoz z bazy...");
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime from = now.minusHours(24);
            LocalDateTime to = now.plusHours(24);
            
            List<Prognoza> forecasts = service.getForecasts(deviceId, from, to);
            System.out.println("   ✓ Znaleziono " + forecasts.size() + " prognoz\n");
            
            if (forecasts.isEmpty()) {
                System.out.println("   Brak prognoz w bazie. Spróbuj wygenerować prognozę najpierw.");
                return;
            }
            
            System.out.println("3. Formatowanie jako JSON...\n");
            System.out.println("=== JSON OUTPUT ===");
            
            ObjectNode response = mapper.createObjectNode();
            response.put("success", true);
            response.put("deviceId", deviceId);
            response.put("count", forecasts.size());
            response.put("periodFrom", from.toString());
            response.put("periodTo", to.toString());
            
            ArrayNode forecastsArray = mapper.createArrayNode();
            for (Prognoza prognoza : forecasts) {
                ObjectNode forecastNode = mapper.createObjectNode();
                forecastNode.put("id", prognoza.getId());
                forecastNode.put("deviceId", prognoza.getUrzadzenieId());
                forecastNode.put("buildingId", prognoza.getBudynekId() != null ? prognoza.getBudynekId() : 0);
                forecastNode.put("czasWygenerowania", prognoza.getCzasWygenerowania().toString());
                forecastNode.put("czasPrognozy", prognoza.getCzasPrognozy().toString());
                forecastNode.put("prognozowanaWartosc", prognoza.getPrognozowanaWartosc());
                forecastNode.put("metryka", prognoza.getMetryka());
                forecastsArray.add(forecastNode);
            }
            response.set("forecasts", forecastsArray);
            
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
            System.out.println(json);
            
            System.out.println("\n=== KONIEC TESTU ===");
            
        } catch (Exception e) {
            System.err.println("BŁĄD: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
