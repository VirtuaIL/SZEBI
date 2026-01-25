package org.example;

import org.example.DTO.Prognoza;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ForecastReportExporter {
    
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    public ForecastReportExporter() {
        this.objectMapper = new ObjectMapper();
    }
    
    public String exportToJSON(List<Prognoza> forecasts, int deviceId, LocalDateTime from, LocalDateTime to) throws IOException {
        ObjectNode report = objectMapper.createObjectNode();
        
        report.put("deviceId", deviceId);
        report.put("generatedAt", LocalDateTime.now().toString());
        report.put("periodFrom", from.toString());
        report.put("periodTo", to.toString());
        report.put("forecastCount", forecasts.size());
        
        ArrayNode forecastsArray = objectMapper.createArrayNode();
        for (Prognoza prognoza : forecasts) {
            ObjectNode forecastNode = objectMapper.createObjectNode();
            forecastNode.put("id", prognoza.getId());
            forecastNode.put("deviceId", prognoza.getUrzadzenieId());
            forecastNode.put("buildingId", prognoza.getBudynekId() != null ? prognoza.getBudynekId() : 0);
            forecastNode.put("czasWygenerowania", prognoza.getCzasWygenerowania().toString());
            forecastNode.put("czasPrognozy", prognoza.getCzasPrognozy().toString());
            forecastNode.put("prognozowanaWartosc", prognoza.getPrognozowanaWartosc());
            forecastNode.put("metryka", prognoza.getMetryka());
            forecastsArray.add(forecastNode);
        }
        report.set("forecasts", forecastsArray);
        
        String filename = "forecast_report_device_" + deviceId + "_" + 
                         LocalDateTime.now().format(DATE_FORMATTER) + ".json";
        
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report));
        }
        
        return filename;
    }
}
