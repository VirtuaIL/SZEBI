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
import org.example.IDocumentGeneratorService;
import org.example.DocumentGenerator;
import org.example.IDocument;

import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class ReportsController {
  private final IAnalyticsData analyticsData;
  private final IControlData controlData;
  private final AnalysisReportAPI analysisReportAPI;
  private final ObjectMapper objectMapper;

  // Przechowywanie referencji do serwisów generatorów
  private final Map<String, IDocumentGeneratorService> activeServices = new HashMap<>();

  // Generator services
  private static class ReportGeneratorService implements IDocumentGeneratorService {
    private final String name;
    private final IDocument.Scheme scheme;
    private final Period period;

    public ReportGeneratorService(String name, IDocument.Scheme scheme, Period period) {
      this.name = name;
      this.scheme = scheme;
      this.period = period;
    }

    @Override
    public List<DocumentGenerator> build(DocumentGenerator.Builder builder) {
      builder.addDocumentConfig(scheme, period);
      DocumentGenerator generator = builder.build();
      // MUSI BYĆ MODYFIKOWALNA LISTA!
      List<DocumentGenerator> generators = new ArrayList<>();
      generators.add(generator);
      return generators;
    }

    public String getName() {
      return name;
    }
  }

  private static class AnalysisGeneratorService implements IDocumentGeneratorService {
    private final String name;
    private final IDocument.Scheme scheme;
    private final Period period;

    public AnalysisGeneratorService(String name, IDocument.Scheme scheme, Period period) {
      this.name = name;
      this.scheme = scheme;
      this.period = period;
    }

    @Override
    public List<DocumentGenerator> build(DocumentGenerator.Builder builder) {
      builder.addDocumentConfig(scheme, period);
      DocumentGenerator generator = builder.build();
      // MUSI BYĆ MODYFIKOWALNA LISTA!
      List<DocumentGenerator> generators = new ArrayList<>();
      generators.add(generator);
      return generators;
    }

    public String getName() {
      return name;
    }
  }

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

    // Zarządzanie generatorami - MUSI BYĆ PRZED /api/reports/{id}
    app.get("/api/reports/generators", this::listGenerators);
    app.delete("/api/reports/generators/{serviceId}/{generatorId}", this::deleteGenerator);

    // Pobierz dostepne metryki
    app.get("/api/reports/metrics", this::getAvailableMetrics);

    // Szczegoly raportu po ID - MUSI BYĆ PO wszystkich bardziej specyficznych
    app.get("/api/reports/{id}", this::getReportById);

    // Generowanie nowego raportu
    app.post("/api/reports/generate", this::generateReport);

    // Eksport raportu do roznych formatow
    app.get("/api/reports/{id}/export/{format}", this::exportReport);

    // Pobierz dane do raportu (bez zapisywania)
    app.post("/api/reports/preview", this::previewReport);

    // Generuj szczegółową analizę
    app.post("/api/reports/analysis", this::generateAnalysis);

    // Dodaj generator raportów/analiz
    app.post("/api/reports/analysis/addGenerator", this::addAnalysisGenerator);
    app.post("/api/reports/addGenerator", this::addReportGenerator);
  }

  private void addAnalysisGenerator(Context ctx) {
    try {
      String body = ctx.body();
      System.out.println("[ReportsController] Add analysis generator request: " + body);

      JsonNode requestBody = objectMapper.readTree(body);

      String name = requestBody.has("name") ? requestBody.get("name").asText() : "Analysis Generator";
      String dateFromStr = requestBody.has("dateFrom") ? requestBody.get("dateFrom").asText() : null;
      String dateToStr = requestBody.has("dateTo") ? requestBody.get("dateTo").asText() : null;

      // Parsuj okres (w dniach, godzinach, minutach)
      int periodDays = requestBody.has("periodDays") ? requestBody.get("periodDays").asInt() : 0;
      int periodMonths = requestBody.has("periodMonths") ? requestBody.get("periodMonths").asInt() : 0;
      int periodYears = requestBody.has("periodYears") ? requestBody.get("periodYears").asInt() : 0;

      if (periodDays == 0 && periodMonths == 0 && periodYears == 0) {
        ctx.status(400);
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", "Musisz podac co najmniej jeden okres (periodDays, periodMonths lub periodYears)");
        ctx.json(error);
        return;
      }

      // Parsuj daty
      LocalDateTime dateFrom = dateFromStr != null && !dateFromStr.isEmpty()
          ? LocalDateTime.parse(dateFromStr + "T00:00:00")
          : LocalDateTime.now().minusDays(7);
      LocalDateTime dateTo = dateToStr != null && !dateToStr.isEmpty()
          ? LocalDateTime.parse(dateToStr + "T23:59:59")
          : LocalDateTime.now();

      // Utwórz schemat analizy
      IDocument.Scheme scheme = AnalysisReportAPI.newAnalysisScheme()
          .setFrom(dateFrom)
          .setTo(dateTo)
          .includeMetrics(AnalysisReportAPI.getAvailableMetrics());

      // Utwórz period
      Period period = Period.of(periodYears, periodMonths, periodDays);

      // Utwórz service generator z konfiguracją
      AnalysisGeneratorService service = new AnalysisGeneratorService(name, scheme, period);

      // Bind generator - to już tworzy i rejestruje generator
      analysisReportAPI.bindDocumentGenerator(service);

      List<DocumentGenerator> generators = analysisReportAPI.getBindedDocumentGenerators(service);

      if (generators == null || generators.isEmpty()) {
        ctx.status(500);
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", "Nie udalo sie stworzyc generatora");
        ctx.json(error);
        return;
      }

      // Inicjalizuj generator (wywołanie package-private przez refleksję lub
      // bezpośrednio jeśli w tym samym pakiecie)
      for (DocumentGenerator gen : generators) {
        try {
          // Wywołaj initDocumentGenerator przez reflection
          java.lang.reflect.Method initMethod = gen.getClass().getDeclaredMethod("initDocumentGenerator");
          initMethod.setAccessible(true);
          boolean initialized = (boolean) initMethod.invoke(gen);
          System.out.println("[ReportsController] Generator initialized: " + initialized);
        } catch (Exception initEx) {
          System.err.println("[ReportsController] Failed to initialize generator: " + initEx.getMessage());
          initEx.printStackTrace();
        }
      }

      // Zapisz service
      String serviceKey = "analysis-" + generators.get(0).getId();
      activeServices.put(serviceKey, service);
      System.out.println("[ReportsController] Added analysis generator with service key: " + serviceKey);

      ObjectNode response = objectMapper.createObjectNode();
      response.put("success", true);
      response.put("message", "Generator analizy zostal dodany");
      response.put("serviceName", name);
      response.put("serviceKey", serviceKey);
      response.put("generatorCount", generators.size());
      if (!generators.isEmpty()) {
        response.put("generatorId", generators.get(0).getId());
      }

      ctx.status(201);
      ctx.json(response);
    } catch (Exception e) {
      System.err.println("[ReportsController] Add analysis generator error: " + e.getMessage());
      e.printStackTrace();
      ctx.status(500);
      ObjectNode error = objectMapper.createObjectNode();
      error.put("error", "Blad podczas dodawania generatora analizy: " + e.getMessage());
      ctx.json(error);
    }
  }

  private void addReportGenerator(Context ctx) {
    try {
      String body = ctx.body();
      System.out.println("[ReportsController] Add report generator request: " + body);

      JsonNode requestBody = objectMapper.readTree(body);

      String name = requestBody.has("name") ? requestBody.get("name").asText() : "Report Generator";
      String dateFromStr = requestBody.has("dateFrom") ? requestBody.get("dateFrom").asText() : null;
      String dateToStr = requestBody.has("dateTo") ? requestBody.get("dateTo").asText() : null;

      // Parsuj okres
      int periodDays = requestBody.has("periodDays") ? requestBody.get("periodDays").asInt() : 0;
      int periodMonths = requestBody.has("periodMonths") ? requestBody.get("periodMonths").asInt() : 0;
      int periodYears = requestBody.has("periodYears") ? requestBody.get("periodYears").asInt() : 0;

      if (periodDays == 0 && periodMonths == 0 && periodYears == 0) {
        ctx.status(400);
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", "Musisz podac co najmniej jeden okres (periodDays, periodMonths lub periodYears)");
        ctx.json(error);
        return;
      }

      // Parsuj daty
      LocalDateTime dateFrom = dateFromStr != null && !dateFromStr.isEmpty()
          ? LocalDateTime.parse(dateFromStr + "T00:00:00")
          : LocalDateTime.now().minusDays(7);
      LocalDateTime dateTo = dateToStr != null && !dateToStr.isEmpty()
          ? LocalDateTime.parse(dateToStr + "T23:59:59")
          : LocalDateTime.now();

      // Utwórz schemat raportu
      IDocument.Scheme scheme = AnalysisReportAPI.newReportScheme()
          .setFrom(dateFrom)
          .setTo(dateTo)
          .includeMetrics(AnalysisReportAPI.getAvailableMetrics());

      // Utwórz period
      Period period = Period.of(periodYears, periodMonths, periodDays);

      // Utwórz service generator z konfiguracją
      ReportGeneratorService service = new ReportGeneratorService(name, scheme, period);

      // Bind generator - to już tworzy i rejestruje generator
      analysisReportAPI.bindDocumentGenerator(service);

      List<DocumentGenerator> generators = analysisReportAPI.getBindedDocumentGenerators(service);

      if (generators == null || generators.isEmpty()) {
        ctx.status(500);
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", "Nie udalo sie stworzyc generatora");
        ctx.json(error);
        return;
      }

      // Inicjalizuj generator
      for (DocumentGenerator gen : generators) {
        try {
          // Wywołaj initDocumentGenerator przez reflection
          java.lang.reflect.Method initMethod = gen.getClass().getDeclaredMethod("initDocumentGenerator");
          initMethod.setAccessible(true);
          boolean initialized = (boolean) initMethod.invoke(gen);
          System.out.println("[ReportsController] Generator initialized: " + initialized);
        } catch (Exception initEx) {
          System.err.println("[ReportsController] Failed to initialize generator: " + initEx.getMessage());
          initEx.printStackTrace();
        }
      }

      // Zapisz service
      String serviceKey = "report-" + generators.get(0).getId();
      activeServices.put(serviceKey, service);
      System.out.println("[ReportsController] Added report generator with service key: " + serviceKey);

      ObjectNode response = objectMapper.createObjectNode();
      response.put("success", true);
      response.put("message", "Generator raportu zostal dodany");
      response.put("serviceName", name);
      response.put("serviceKey", serviceKey);
      response.put("generatorCount", generators.size());
      if (!generators.isEmpty()) {
        response.put("generatorId", generators.get(0).getId());
      }

      ctx.status(201);
      ctx.json(response);
    } catch (Exception e) {
      System.err.println("[ReportsController] Add report generator error: " + e.getMessage());
      e.printStackTrace();
      ctx.status(500);
      ObjectNode error = objectMapper.createObjectNode();
      error.put("error", "Blad podczas dodawania generatora raportu: " + e.getMessage());
      ctx.json(error);
    }
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

      String reportType = requestBody.has("type") ? requestBody.get("type").asText() : "Raport";
      String dateFromStr = requestBody.has("dateFrom") ? requestBody.get("dateFrom").asText() : null;
      String dateToStr = requestBody.has("dateTo") ? requestBody.get("dateTo").asText() : null;

      // NOWE: Pobranie medium z żądania
      String medium = requestBody.has("medium") ? requestBody.get("medium").asText() : "all";

      System.out.println("[ReportsController] Generate params: type=" + reportType +
          ", dateFrom=" + dateFromStr + ", dateTo=" + dateToStr + ", medium=" + medium);

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
        ctx.status(400).json(objectMapper.createObjectNode().put("error", "Nieprawidlowy format daty."));
        return;
      }

      // NOWE: Filtrowanie metryk na podstawie wybranego medium
      Set<ConfigurationType> allAvailableMetrics = AnalysisReportAPI.getAvailableMetrics();
      System.out.println("[INFO] Metrics Count " + allAvailableMetrics.stream().count());

      Set<ConfigurationType> filteredMetrics = allAvailableMetrics.stream()
          .collect(Collectors.toSet());

      if (filteredMetrics.isEmpty()) {
        System.out.println("[WARNING] No metrics found for medium: " + medium);
        // Opcjonalnie: jeśli nie znaleziono specyficznych, weź wszystkie, by raport nie
        // był pusty
        filteredMetrics = allAvailableMetrics;
      }

      // Budowanie schematu z uwzględnieniem filtrowania
      var scheme = AnalysisReportAPI.newReportScheme()
          .setFrom(dateFrom)
          .setTo(dateTo)
          .includeMetrics(filteredMetrics); // Używamy przefiltrowanej listy

      analysisReportAPI.sendDocumentScheme(scheme);

      // Reszta logiki pobierania z bazy...
      List<Raport> recentReports = analyticsData.getReportsByType(reportType);
      if (recentReports == null || recentReports.isEmpty()) {
        ctx.status(500).json(objectMapper.createObjectNode().put("error", "Nie udalo sie pobrac zapisanego raportu"));
        return;
      }

      Raport savedReport = recentReports.stream()
          .max((r1, r2) -> r1.getCzasWygenerowania().compareTo(r2.getCzasWygenerowania()))
          .orElse(recentReports.get(0));

      ObjectNode response = objectMapper.createObjectNode();
      response.put("success", true);
      response.set("report", convertReportToJson(savedReport));

      ctx.status(201).json(response);
    } catch (Exception e) {
      e.printStackTrace();
      ctx.status(500).json(objectMapper.createObjectNode().put("error", e.getMessage()));
    }
  }

  private void previewReport(Context ctx) {
    try {
      String body = ctx.body();
      System.out.println("[ReportsController] Preview request body: " + body);

      JsonNode requestBody = objectMapper.readTree(body);

      String reportType = requestBody.has("type") ? requestBody.get("type").asText() : "Raport";

      System.out.println("[ReportsController] Preview params: type=" + reportType);

      // Pobierz najnowszy raport z bazy danych
      List<Raport> reports = analyticsData.getReportsByType(reportType);

      if (reports == null || reports.isEmpty()) {
        ctx.status(404);
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", "Brak raportow tego typu w bazie danych");
        error.put("suggestion", "Najpierw wygeneruj raport");
        ctx.json(error);
        return;
      }

      // Znajdz najnowszy raport
      Raport latestReport = reports.stream()
          .max((r1, r2) -> r1.getCzasWygenerowania().compareTo(r2.getCzasWygenerowania()))
          .orElse(reports.get(0));

      // Opakowuje w format oczekiwany przez frontend
      ObjectNode reportContent = objectMapper.createObjectNode();
      reportContent.put("reportType", reportType);
      reportContent.set("data", objectMapper.readTree(latestReport.getZawartosc()));
      reportContent.put("id", latestReport.getId());
      reportContent.put("generatedAt",
          latestReport.getCzasWygenerowania().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

      ctx.status(200);
      ctx.json(reportContent);
    } catch (Exception e) {
      System.err.println("[ReportsController] Preview error: " + e.getClass().getName() + ": " + e.getMessage());
      e.printStackTrace();
      ctx.status(500);
      ObjectNode error = objectMapper.createObjectNode();
      error.put("error",
          "Blad podczas pobierania podgladu raportu: " + e.getClass().getSimpleName() + " - " + e.getMessage());
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
      for (ConfigurationType metric : AnalysisReportAPI.getAvailableMetrics()) {
        metricsArray.add(metric.toString());
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

  private void generateAnalysis(Context ctx) {
    try {
      String body = ctx.body();
      System.out.println("[ReportsController] Analysis request body: " + body);

      JsonNode requestBody = objectMapper.readTree(body);

      String dateFromStr = requestBody.has("dateFrom") ? requestBody.get("dateFrom").asText() : null;
      String dateToStr = requestBody.has("dateTo") ? requestBody.get("dateTo").asText() : null;

      System.out.println("[ReportsController] Analysis params: dateFrom=" + dateFromStr +
          ", dateTo=" + dateToStr);

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

      // Użyj AnalysisReportAPI.sendDocumentScheme do wygenerowania i zapisania
      // analizy
      var scheme = AnalysisReportAPI.newAnalysisScheme()
          .setFrom(dateFrom)
          .setTo(dateTo)
          .includeMetrics(AnalysisReportAPI.getAvailableMetrics());

      System.out.println("[ReportsController] Wysyłanie schematu analizy...");
      analysisReportAPI.sendDocumentScheme(scheme);

      // Daj chwilę na zapisanie do bazy (może być asynchroniczne)
      try {
        Thread.sleep(100);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }

      // Pobierz ostatnio wygenerowaną analizę z bazy
      System.out.println("[ReportsController] Pobieranie analizy z bazy...");
      List<Raport> recentAnalyses = null;
      try {
        recentAnalyses = analyticsData.getReportsByType("Analiza");
        System.out
            .println("[ReportsController] Znaleziono analiz: " + (recentAnalyses != null ? recentAnalyses.size() : 0));
      } catch (Exception dbEx) {
        System.err.println("[ReportsController] Błąd pobierania z bazy: " + dbEx.getMessage());
        dbEx.printStackTrace();
      }

      if (recentAnalyses == null || recentAnalyses.isEmpty()) {
        ctx.status(500);
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", "Nie udalo sie pobrac zapisanej analizy z bazy danych");
        error.put("hint", "Analiza mogla zostac wygenerowana ale nie zapisana w bazie");
        ctx.json(error);
        return;
      }

      // Znajdz najnowszą analizę
      Raport savedAnalysis = recentAnalyses.stream()
          .max((r1, r2) -> r1.getCzasWygenerowania().compareTo(r2.getCzasWygenerowania()))
          .orElse(recentAnalyses.get(0));

      System.out.println("[ReportsController] Najnowsza analiza ID: " + savedAnalysis.getId());
      System.out.println("[ReportsController] Zawartość (pierwsze 200 znaków): "
          + savedAnalysis.getZawartosc().substring(0, Math.min(200, savedAnalysis.getZawartosc().length())));

      // Parsuj zawartosc i zwróć
      JsonNode analysisResult = objectMapper.readTree(savedAnalysis.getZawartosc());

      ctx.status(200);
      ctx.json(analysisResult);

    } catch (Exception e) {
      System.err.println("[ReportsController] Analysis error: " + e.getClass().getName() + ": " + e.getMessage());
      e.printStackTrace();
      ctx.status(500);
      ObjectNode error = objectMapper.createObjectNode();
      error.put("error", "Blad podczas generowania analizy: " + e.getClass().getSimpleName() + " - " + e.getMessage());
      ctx.json(error);
    }
  }

  private void listGenerators(Context ctx) {
    try {
      System.out.println("[ReportsController] Listing generators. Active services: " + activeServices.size());
      ArrayNode generatorsArray = objectMapper.createArrayNode();

      for (Map.Entry<String, IDocumentGeneratorService> entry : activeServices.entrySet()) {
        String serviceKey = entry.getKey();
        IDocumentGeneratorService service = entry.getValue();

        System.out.println("[ReportsController]   Service key: " + serviceKey);

        List<DocumentGenerator> generators = analysisReportAPI.getBindedDocumentGenerators(service);
        System.out.println("[ReportsController]   Generators: " + (generators != null ? generators.size() : "null"));

        for (DocumentGenerator generator : generators) {
          ObjectNode generatorJson = objectMapper.createObjectNode();
          generatorJson.put("id", generator.getId());
          generatorJson.put("serviceKey", serviceKey);
          generatorJson.put("type", serviceKey.startsWith("analysis-") ? "analysis" : "report");

          if (service instanceof AnalysisGeneratorService) {
            generatorJson.put("name", ((AnalysisGeneratorService) service).getName());
          } else if (service instanceof ReportGeneratorService) {
            generatorJson.put("name", ((ReportGeneratorService) service).getName());
          }

          generatorJson.put("schemeCount", generator.getDocumentSchemes().size());

          // Dodaj informacje o okresach
          ArrayNode periodsArray = objectMapper.createArrayNode();
          for (var configEntry : generator.getDocumentConfigs().entrySet()) {
            for (Period period : configEntry.getValue()) {
              ObjectNode periodJson = objectMapper.createObjectNode();
              periodJson.put("years", period.getYears());
              periodJson.put("months", period.getMonths());
              periodJson.put("days", period.getDays());
              periodsArray.add(periodJson);
            }
          }
          generatorJson.set("periods", periodsArray);

          generatorsArray.add(generatorJson);
        }
      }

      ctx.status(200);
      ctx.json(generatorsArray);
    } catch (Exception e) {
      System.err.println("[ReportsController] List generators error: " + e.getMessage());
      e.printStackTrace();
      ctx.status(500);
      ObjectNode error = objectMapper.createObjectNode();
      error.put("error", "Blad podczas pobierania listy generatorow: " + e.getMessage());
      ctx.json(error);
    }
  }

  private void deleteGenerator(Context ctx) {
    try {
      String serviceId = ctx.pathParam("serviceId");
      String generatorId = ctx.pathParam("generatorId");

      System.out
          .println("[ReportsController] Delete generator: serviceId=" + serviceId + ", generatorId=" + generatorId);
      System.out.println("[ReportsController] Active services: " + activeServices.keySet());

      IDocumentGeneratorService service = activeServices.get(serviceId);

      if (service == null) {
        ctx.status(404);
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", "Generator service nie zostal znaleziony: " + serviceId);
        error.put("availableServices", activeServices.keySet().toString());
        ctx.json(error);
        return;
      }

      List<DocumentGenerator> generators = analysisReportAPI.getBindedDocumentGenerators(service);

      if (generators == null || generators.isEmpty()) {
        ctx.status(404);
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", "Brak generatorow dla tego serwisu");
        ctx.json(error);
        return;
      }

      System.out.println("[ReportsController] Found " + generators.size() + " generators");
      generators.forEach(g -> System.out.println("  - Generator ID: " + g.getId()));

      DocumentGenerator targetGenerator = generators.stream()
          .filter(g -> g.getId().equals(generatorId))
          .findFirst()
          .orElse(null);

      if (targetGenerator == null) {
        ctx.status(404);
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", "Generator nie zostal znaleziony w liscie");
        error.put("searchedId", generatorId);
        error.put("availableIds", generators.stream().map(DocumentGenerator::getId).toList().toString());
        ctx.json(error);
        return;
      }

      boolean removed = analysisReportAPI.unBindDocumentGenerator(service, targetGenerator);

      if (removed) {
        activeServices.remove(serviceId);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("success", true);
        response.put("message", "Generator zostal usuniety");

        ctx.status(200);
        ctx.json(response);
      } else {
        ctx.status(500);
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", "Nie udalo sie usunac generatora");
        ctx.json(error);
      }
    } catch (Exception e) {
      System.err.println("[ReportsController] Delete generator error: " + e.getMessage());
      e.printStackTrace();
      ctx.status(500);
      ObjectNode error = objectMapper.createObjectNode();
      error.put("error", "Blad podczas usuwania generatora: " + e.getMessage());
      ctx.json(error);
    }
  }
}
