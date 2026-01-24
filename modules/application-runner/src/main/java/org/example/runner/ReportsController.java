package org.example.runner;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.interfaces.IAnalyticsData;
import org.example.interfaces.IControlData;
import org.example.DTO.Raport;
import org.example.DTO.Odczyt;
import org.example.DTO.AlertSzczegoly;
import org.example.DTO.Umowa;
import org.example.AnalysisReportAPI;
import org.example.ConfigurationType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class ReportsController {
  private final IAnalyticsData analyticsData;
  private final IControlData controlData;
  private final AnalysisReportAPI analysisReportAPI;
  private final ObjectMapper objectMapper;

  public ReportsController(IAnalyticsData analyticsData, IControlData controlData,
      AnalysisReportAPI analysisReportAPI) {
    this.analyticsData = analyticsData;
    this.controlData = controlData;
    this.analysisReportAPI = analysisReportAPI;
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
    this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  public void setupRoutes(Javalin app) {
    // Lista raportow z opcjonalnymi filtrami
    app.get("/api/reports", this::getAllReports);

    // Szczegoly raportu po ID
    app.get("/api/reports/{id}", this::getReportById);

    // Generowanie nowego raportu
    app.post("/api/reports/generate", this::generateReport);

    // Eksport raportu do roznych formatow
    app.get("/api/reports/{id}/export/{format}", this::exportReport);

    // Pobierz dane do raportu (bez zapisywania)
    app.post("/api/reports/preview", this::previewReport);

    // Pobierz dostepne metryki
    app.get("/api/reports/metrics", this::getAvailableMetrics);
  }

  private void getAllReports(Context ctx) {
    try {
      String reportType = ctx.queryParam("type");
      List<Raport> reports = new ArrayList<>();

      try {
        if (reportType != null && !reportType.isEmpty()) {
          reports = analyticsData.getReportsByType(reportType);
        } else {
          String[] types = { "Raport", "Analiza", "energy", "alerts", "devices", "costs" };
          for (String type : types) {
            reports.addAll(analyticsData.getReportsByType(type));
          }
        }
      } catch (Exception dbEx) {
        System.out.println("[ReportsController] Brak tabeli Raporty w bazie");
      }

      ArrayNode reportsArray = objectMapper.createArrayNode();
      for (Raport report : reports) {
        reportsArray.add(convertReportToJson(report));
      }

      ctx.status(200);
      ctx.json(reportsArray);
    } catch (Exception e) {
      ctx.status(500);
      ObjectNode error = objectMapper.createObjectNode();
      error.put("error", "Blad podczas pobierania raportow: " + e.getMessage());
      ctx.json(error);
      e.printStackTrace();
    }
  }

  private void getReportById(Context ctx) {
    try {
      int reportId = ctx.pathParamAsClass("id", Integer.class).get();

      Raport report = analyticsData.getReportById(reportId);

      if (report == null) {
        ctx.status(404);
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", "Raport o ID " + reportId + " nie zostal znaleziony");
        ctx.json(error);
        return;
      }

      ctx.status(200);
      ctx.json(convertReportToJson(report));
    } catch (Exception e) {
      ctx.status(500);
      ObjectNode error = objectMapper.createObjectNode();
      error.put("error", "Blad podczas pobierania raportu: " + e.getMessage());
      ctx.json(error);
      e.printStackTrace();
    }
  }

  private void generateReport(Context ctx) {
    try {
      String body = ctx.body();
      System.out.println("[ReportsController] Generate request body: " + body);

      JsonNode requestBody = objectMapper.readTree(body);

      String reportType = requestBody.has("type") ? requestBody.get("type").asText() : "energy";
      String dateFromStr = requestBody.has("dateFrom") ? requestBody.get("dateFrom").asText() : null;
      String dateToStr = requestBody.has("dateTo") ? requestBody.get("dateTo").asText() : null;
      int buildingId = requestBody.has("buildingId") ? requestBody.get("buildingId").asInt() : 1;
      String zone = requestBody.has("zone") ? requestBody.get("zone").asText() : "all";
      String medium = requestBody.has("medium") ? requestBody.get("medium").asText() : "";
      Integer userId = requestBody.has("userId") && !requestBody.get("userId").isNull()
          ? requestBody.get("userId").asInt()
          : null;

      System.out.println("[ReportsController] Generate params: type=" + reportType + ", dateFrom=" + dateFromStr +
          ", dateTo=" + dateToStr + ", buildingId=" + buildingId);

      // Parsuj daty
      LocalDateTime dateFrom;
      LocalDateTime dateTo;

      try {
        dateFrom = dateFromStr != null && !dateFromStr.isEmpty()
            ? LocalDateTime.parse(dateFromStr + "T00:00:00")
            : LocalDateTime.now().minusDays(7);
        dateTo = dateToStr != null && !dateToStr.isEmpty()
            ? LocalDateTime.parse(dateToStr + "T23:59:59")
            : LocalDateTime.now();
      } catch (Exception parseEx) {
        System.err.println("[ReportsController] Date parsing error: " + parseEx.getMessage());
        ctx.status(400);
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", "Nieprawidlowy format daty. Uzyj formatu YYYY-MM-DD");
        ctx.json(error);
        return;
      }

      // Generuj zawartosc raportu w zaleznosci od typu
      ObjectNode reportContent = generateReportContent(reportType, buildingId, dateFrom, dateTo, zone, medium);

      // Utworz raport
      Raport report = new Raport();
      report.setTypRaportu(reportType);
      report.setUzytkownikId(userId);
      report.setCzasWygenerowania(LocalDateTime.now());
      report.setZakresOd(dateFrom);
      report.setZakresDo(dateTo);
      report.setOpis(generateReportDescription(reportType, zone, medium));
      report.setZawartosc(objectMapper.writeValueAsString(reportContent));

      // Zapisz raport do bazy
      Raport savedReport = analyticsData.saveReport(report);

      if (savedReport == null) {
        ctx.status(500);
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", "Nie udalo sie zapisac raportu");
        ctx.json(error);
        return;
      }

      ObjectNode response = objectMapper.createObjectNode();
      response.put("success", true);
      response.put("message", "Raport zostal wygenerowany");
      response.set("report", convertReportToJson(savedReport));

      ctx.status(201);
      ctx.json(response);
    } catch (Exception e) {
      System.err.println("[ReportsController] Generate error: " + e.getClass().getName() + ": " + e.getMessage());
      e.printStackTrace();
      ctx.status(500);
      ObjectNode error = objectMapper.createObjectNode();
      error.put("error", "Blad podczas generowania raportu: " + e.getClass().getSimpleName() + " - " + e.getMessage());
      ctx.json(error);
    }
  }

  private void previewReport(Context ctx) {
    try {
      String body = ctx.body();
      System.out.println("[ReportsController] Preview request body: " + body);

      JsonNode requestBody = objectMapper.readTree(body);

      String reportType = requestBody.has("type") ? requestBody.get("type").asText() : "energy";
      String dateFromStr = requestBody.has("dateFrom") ? requestBody.get("dateFrom").asText() : null;
      String dateToStr = requestBody.has("dateTo") ? requestBody.get("dateTo").asText() : null;
      int buildingId = requestBody.has("buildingId") ? requestBody.get("buildingId").asInt() : 1;
      String zone = requestBody.has("zone") ? requestBody.get("zone").asText() : "all";
      String medium = requestBody.has("medium") ? requestBody.get("medium").asText() : "";

      System.out.println("[ReportsController] Params: type=" + reportType + ", dateFrom=" + dateFromStr +
          ", dateTo=" + dateToStr + ", buildingId=" + buildingId);

      LocalDateTime dateFrom;
      LocalDateTime dateTo;

      try {
        dateFrom = dateFromStr != null && !dateFromStr.isEmpty()
            ? LocalDateTime.parse(dateFromStr + "T00:00:00")
            : LocalDateTime.now().minusDays(7);
        dateTo = dateToStr != null && !dateToStr.isEmpty()
            ? LocalDateTime.parse(dateToStr + "T23:59:59")
            : LocalDateTime.now();
      } catch (Exception parseEx) {
        System.err.println("[ReportsController] Date parsing error: " + parseEx.getMessage());
        ctx.status(400);
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", "Nieprawidlowy format daty. Uzyj formatu YYYY-MM-DD");
        ctx.json(error);
        return;
      }

      ObjectNode reportContent = generateReportContent(reportType, buildingId, dateFrom, dateTo, zone, medium);

      ctx.status(200);
      ctx.json(reportContent);
    } catch (Exception e) {
      System.err.println("[ReportsController] Preview error: " + e.getClass().getName() + ": " + e.getMessage());
      e.printStackTrace();
      ctx.status(500);
      ObjectNode error = objectMapper.createObjectNode();
      error.put("error",
          "Blad podczas generowania podgladu raportu: " + e.getClass().getSimpleName() + " - " + e.getMessage());
      ctx.json(error);
    }
  }

  private void exportReport(Context ctx) {
    try {
      int reportId = ctx.pathParamAsClass("id", Integer.class).get();
      String format = ctx.pathParam("format").toUpperCase();

      Raport report = analyticsData.getReportById(reportId);

      if (report == null) {
        ctx.status(404);
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", "Raport o ID " + reportId + " nie zostal znaleziony");
        ctx.json(error);
        return;
      }

      switch (format) {
        case "JSON":
          ctx.contentType("application/json");
          ctx.header("Content-Disposition", "attachment; filename=\"raport_" + reportId + ".json\"");
          ctx.result(report.getZawartosc());
          break;

        case "XML":
          ctx.contentType("application/xml");
          ctx.header("Content-Disposition", "attachment; filename=\"raport_" + reportId + ".xml\"");
          ctx.result(convertReportToXml(report));
          break;

        case "PDF":
          // PDF wymaga dodatkowej biblioteki - zwracamy metadane
          ctx.status(501);
          ObjectNode error = objectMapper.createObjectNode();
          error.put("error", "Eksport PDF nie jest jeszcze zaimplementowany");
          error.put("suggestion", "Uzyj formatu JSON lub XML");
          ctx.json(error);
          break;

        default:
          ctx.status(400);
          ObjectNode badFormat = objectMapper.createObjectNode();
          badFormat.put("error", "Nieobslugiwany format: " + format);
          badFormat.put("supportedFormats", "JSON, XML, PDF");
          ctx.json(badFormat);
      }
    } catch (Exception e) {
      ctx.status(500);
      ObjectNode error = objectMapper.createObjectNode();
      error.put("error", "Blad podczas eksportu raportu: " + e.getMessage());
      ctx.json(error);
      e.printStackTrace();
    }
  }

  private void getAvailableMetrics(Context ctx) {
    try {
      ArrayNode metricsArray = objectMapper.createArrayNode();

      // Pobierz dostepne metryki z AnalysisReportAPI
      for (String metric : AnalysisReportAPI.getAvailableMetrics()) {
        metricsArray.add(metric);
      }

      ctx.status(200);
      ctx.json(metricsArray);
    } catch (Exception e) {
      ctx.status(500);
      ObjectNode error = objectMapper.createObjectNode();
      error.put("error", "Blad podczas pobierania metryk: " + e.getMessage());
      ctx.json(error);
      e.printStackTrace();
    }
  }

  private ObjectNode generateReportContent(String reportType, int buildingId,
      LocalDateTime from, LocalDateTime to, String zone, String mediumStr) {

    ObjectNode content = objectMapper.createObjectNode();
    content.put("reportType", reportType);
    content.put("buildingId", buildingId);
    content.put("zone", zone);
    content.put("medium", mediumStr);
    content.put("dateFrom", from.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    content.put("dateTo", to.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    content.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

    ConfigurationType configurationType = ConfigurationType.fromString(mediumStr);

    switch (reportType) {
      case "energy":
        content.set("data", generateEnergyReportData(buildingId, from, to, configurationType));
        break;
      case "alerts":
        content.set("data", generateAlertsReportData(buildingId, from, to));
        break;
      case "devices":
        content.set("data", generateDevicesReportData(buildingId, from, to));
        break;
      case "costs":
        content.set("data", generateCostsReportData(buildingId, from, to));
        break;
      default:
        content.set("data", generateEnergyReportData(buildingId, from, to, configurationType));
    }

    return content;
  }

  private ArrayNode generateEnergyReportData(int buildingId, LocalDateTime from, LocalDateTime to, ConfigurationType configurationType) {
    ArrayNode dataArray = objectMapper.createArrayNode();

    try {
      List<Odczyt> readings = analyticsData.getReadingsForBuilding(buildingId, from, to);

      if (readings == null || readings.isEmpty()) {
        System.out.println(
            "[ReportsController] Brak odczytow dla budynku " + buildingId + " w zakresie " + from + " - " + to);
        return dataArray;
      }

      System.out.println("[ReportsController] Znaleziono " + readings.size() + " odczytow");

      // Grupuj odczyty po godzinie
      Map<String, List<Double>> hourlyData = new HashMap<>();

      for (Odczyt reading : readings) {
        if (reading == null || reading.getCzasOdczytu() == null) {
          continue;
        }
        LocalDateTime readingTime = LocalDateTime.ofInstant(reading.getCzasOdczytu(), ZoneId.systemDefault());
        String hourKey = readingTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00"));

        double value = extractValueFromReading(reading, configurationType);

        hourlyData.computeIfAbsent(hourKey, k -> new ArrayList<>()).add(value);
      }

      // Przeksztalc na format wynikowy - sortuj po czasie
      List<String> sortedKeys = new ArrayList<>(hourlyData.keySet());
      sortedKeys.sort(String::compareTo);

      for (String hourKey : sortedKeys) {
        List<Double> values = hourlyData.get(hourKey);
        ObjectNode dataPoint = objectMapper.createObjectNode();
        dataPoint.put("time", hourKey);

        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        double avg = sum / values.size();

        dataPoint.put("value", Math.round(avg * 100.0) / 100.0);
        dataPoint.put("count", values.size());

        // Wykrywanie anomalii - prosta heurystyka (wartosci poza normalnym zakresem)
        boolean isAnomaly = avg > 1000 || avg < 50;
        dataPoint.put("isAnomaly", isAnomaly);

        dataArray.add(dataPoint);
      }

    } catch (Exception e) {
      System.err.println("[ReportsController] Blad generowania danych energii: " + e.getMessage());
      e.printStackTrace();
    }

    return dataArray;
  }

  private ArrayNode generateAlertsReportData(int buildingId, LocalDateTime from, LocalDateTime to) {
    ArrayNode dataArray = objectMapper.createArrayNode();

    try {
      List<AlertSzczegoly> alerts = analyticsData.getAlertDetailsForBuilding(buildingId, from, to);

      if (alerts == null) {
        alerts = new ArrayList<>();
      }

      // Statystyki alertow
      int critical = 0, warning = 0, info = 0;
      int resolved = 0, confirmed = 0, newAlerts = 0;

      for (AlertSzczegoly alert : alerts) {
        if (alert == null)
          continue;

        ObjectNode alertNode = objectMapper.createObjectNode();
        alertNode.put("id", alert.getId());
        alertNode.put("message", alert.getTresc() != null ? alert.getTresc() : "");
        alertNode.put("priority", alert.getPriorytet() != null ? alert.getPriorytet().name() : "INFO");
        alertNode.put("status", alert.getStatus() != null ? alert.getStatus().name() : "NOWY");
        alertNode.put("deviceName", alert.getNazwaUrzadzenia() != null ? alert.getNazwaUrzadzenia() : "Nieznane");
        alertNode.put("location", alert.getNazwaPokoju() != null ? alert.getNazwaPokoju() : "");
        if (alert.getCzasAlertu() != null) {
          alertNode.put("timestamp", alert.getCzasAlertu().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        dataArray.add(alertNode);

        // Zliczaj statystyki
        if (alert.getPriorytet() != null) {
          switch (alert.getPriorytet()) {
            case CRITICAL:
              critical++;
              break;
            case WARNING:
              warning++;
              break;
            case INFO:
              info++;
              break;
          }
        }

        if (alert.getStatus() != null) {
          switch (alert.getStatus()) {
            case ROZWIAZANY:
              resolved++;
              break;
            case POTWIERDZONY:
              confirmed++;
              break;
            case NOWY:
              newAlerts++;
              break;
          }
        }
      }

      ObjectNode summary = objectMapper.createObjectNode();
      summary.put("type", "summary");
      summary.put("total", alerts.size());
      summary.put("critical", critical);
      summary.put("warning", warning);
      summary.put("info", info);
      summary.put("resolved", resolved);
      summary.put("confirmed", confirmed);
      summary.put("new", newAlerts);

      ArrayNode result = objectMapper.createArrayNode();
      result.add(summary);
      result.addAll(dataArray);

      return result;

    } catch (Exception e) {
      e.printStackTrace();
      return dataArray;
    }
  }

  private ArrayNode generateDevicesReportData(int buildingId, LocalDateTime from, LocalDateTime to) {
    ArrayNode dataArray = objectMapper.createArrayNode();

    try {
      List<Odczyt> readings = analyticsData.getReadingsForBuilding(buildingId, from, to);

      if (readings == null || readings.isEmpty()) {
        return dataArray;
      }

      // Grupuj po urzadzeniu
      Map<Integer, List<Odczyt>> deviceReadings = new HashMap<>();
      for (Odczyt reading : readings) {
        if (reading != null) {
          deviceReadings.computeIfAbsent(reading.getUrzadzenieId(), k -> new ArrayList<>()).add(reading);
        }
      }

      for (Map.Entry<Integer, List<Odczyt>> entry : deviceReadings.entrySet()) {
        ObjectNode deviceNode = objectMapper.createObjectNode();
        deviceNode.put("deviceId", entry.getKey());
        deviceNode.put("readingsCount", entry.getValue().size());

        double sum = 0;
        for (Odczyt r : entry.getValue()) {
          sum += extractValueFromReading(r, null);
        }
        double avg = entry.getValue().isEmpty() ? 0 : sum / entry.getValue().size();
        deviceNode.put("averageValue", Math.round(avg * 100.0) / 100.0);

        dataArray.add(deviceNode);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

    return dataArray;
  }

  private ArrayNode generateCostsReportData(int buildingId, LocalDateTime from, LocalDateTime to) {
    ArrayNode dataArray = objectMapper.createArrayNode();

    try {
      double totalEnergy = analyticsData.getTotalEnergyConsumptionForBuilding(buildingId, from, to);

      // TODO KINASIOWI PRZEKAZAĆ ŻEBY KONTROLKE CENY DODAŁ BO NIE MA CO DO BAZY
      double ratePerKwh = 0.85;
      String currency = "PLN";
      String tariffName = "domyslna";

      try {
        Umowa contract = controlData.getActiveContractForBuilding(buildingId);
        if (contract != null && contract.getSzczegolyTaryfy() != null) {
          JsonNode tariffDetails = objectMapper.readTree(contract.getSzczegolyTaryfy());
          if (tariffDetails.has("waluta")) {
            currency = tariffDetails.get("waluta").asText();
          }
          if (tariffDetails.has("taryfy") && tariffDetails.get("taryfy").isArray()) {
            JsonNode tariffs = tariffDetails.get("taryfy");
            if (tariffs.size() > 0) {
              JsonNode firstTariff = tariffs.get(0);
              if (firstTariff.has("cena_za_kwh")) {
                ratePerKwh = firstTariff.get("cena_za_kwh").asDouble();
              }
              if (firstTariff.has("nazwa")) {
                tariffName = firstTariff.get("nazwa").asText();
              }
            }
          }
        }
      } catch (Exception contractEx) {
        System.out.println("[ReportsController] Nie udalo sie pobrac umowy - uzywam domyslnej stawki");
      }

      ObjectNode costNode = objectMapper.createObjectNode();
      costNode.put("totalEnergyKwh", Math.round(totalEnergy * 100.0) / 100.0);
      costNode.put("ratePerKwh", ratePerKwh);
      costNode.put("tariffName", tariffName);
      costNode.put("totalCostPln", Math.round(totalEnergy * ratePerKwh * 100.0) / 100.0);
      costNode.put("currency", currency);

      dataArray.add(costNode);

    } catch (Exception e) {
      System.err.println("[ReportsController] Blad generowania danych kosztow: " + e.getMessage());
      e.printStackTrace();
    }

    return dataArray;
  }

  private double extractValueFromReading(Odczyt reading, ConfigurationType configurationType) {
    if (reading.getPomiary() == null || reading.getPomiary().isEmpty()) {
      return 0.0;
    }

    try {
      JsonNode pomiary = objectMapper.readTree(reading.getPomiary());

      // Szukaj wartosci w zaleznosci od configuration type
      String[] keysToTry;
      if (configurationType == null) {
        keysToTry = new String[] { "wartosc", "value", "moc_W", "temperatura_C", "jasnosc_procent" };
      } else {
        switch (configurationType) {
          case Power:
            keysToTry = new String[] { "moc_W", "power", "zuzycie_kwh", "wartosc" };
            break;
          case Temperature:
            keysToTry = new String[] { "temperatura_C", "temperature", "temp" };
            break;
          case Humidity:
            keysToTry = new String[] { "wilgotnosc_procent", "humidity", "wilgotnosc" };
            break;
          case Pressure:
            keysToTry = new String[] { "cisnienie_hPa", "pressure", "cisnienie" };
            break;
          case Luminosity:
            keysToTry = new String[] { "jasnosc_procent", "luminosity", "jasnosc" };
            break;
          case CO2Level:
            keysToTry = new String[] { "co2_ppm", "co2_level", "co2" };
            break;
          case NoiseLevel:
            keysToTry = new String[] { "halas_db", "noise_level", "halas" };
            break;
          default:
            keysToTry = new String[] { "wartosc", "value", "moc_W", "temperatura_C", "jasnosc_procent" };
        }
      }

      for (String key : keysToTry) {
        if (pomiary.has(key)) {
          return pomiary.get(key).asDouble();
        }
      }

    } catch (Exception e) {
    }

    return 0.0;
  }

  private String generateReportDescription(String reportType, String zone, String mediumStr) {
    StringBuilder desc = new StringBuilder();

    switch (reportType) {
      case "energy":
        desc.append("Raport zuzycia energii");
        break;
      case "alerts":
        desc.append("Raport alarmow");
        break;
      case "devices":
        desc.append("Raport stanu urzadzen");
        break;
      case "costs":
        desc.append("Raport kosztow");
        break;
      default:
        desc.append("Raport ogolny");
    }

    if (!"all".equals(zone)) {
      desc.append(" dla strefy: ").append(zone);
    }

    if (mediumStr != null && !mediumStr.isEmpty()) {
      desc.append(" (medium: ").append(mediumStr).append(")");
    }

    return desc.toString();
  }

  private ObjectNode convertReportToJson(Raport report) {
    ObjectNode reportJson = objectMapper.createObjectNode();
    reportJson.put("id", report.getId());

    if (report.getUzytkownikId() != null) {
      reportJson.put("userId", report.getUzytkownikId());
    }

    reportJson.put("type", report.getTypRaportu());
    reportJson.put("description", report.getOpis());

    if (report.getCzasWygenerowania() != null) {
      reportJson.put("generatedAt", report.getCzasWygenerowania().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    if (report.getZakresOd() != null) {
      reportJson.put("dateFrom", report.getZakresOd().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    if (report.getZakresDo() != null) {
      reportJson.put("dateTo", report.getZakresDo().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    // Parsuj zawartosc jako JSON jesli to mozliwe
    if (report.getZawartosc() != null && !report.getZawartosc().isEmpty()) {
      try {
        JsonNode contentNode = objectMapper.readTree(report.getZawartosc());
        reportJson.set("content", contentNode);
      } catch (Exception e) {
        reportJson.put("content", report.getZawartosc());
      }
    }

    return reportJson;
  }

  private String convertReportToXml(Raport report) {
    StringBuilder xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xml.append("<report>\n");
    xml.append("  <id>").append(report.getId()).append("</id>\n");
    xml.append("  <type>").append(escapeXml(report.getTypRaportu())).append("</type>\n");
    xml.append("  <description>").append(escapeXml(report.getOpis())).append("</description>\n");

    if (report.getCzasWygenerowania() != null) {
      xml.append("  <generatedAt>").append(report.getCzasWygenerowania().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
          .append("</generatedAt>\n");
    }

    if (report.getZakresOd() != null) {
      xml.append("  <dateFrom>").append(report.getZakresOd().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
          .append("</dateFrom>\n");
    }

    if (report.getZakresDo() != null) {
      xml.append("  <dateTo>").append(report.getZakresDo().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
          .append("</dateTo>\n");
    }

    if (report.getZawartosc() != null) {
      xml.append("  <content><![CDATA[").append(report.getZawartosc()).append("]]></content>\n");
    }

    xml.append("</report>");
    return xml.toString();
  }

  private String escapeXml(String input) {
    if (input == null)
      return "";
    return input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }
}
