package org.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

final class Analysis extends IDocument.Document {
  private IAnalysisStrategy strategy = new IAnalysisStrategy.DefaultStrategy();
  private List<AlertEventType> alerts = new ArrayList<>();

  public Analysis(IDocument.Scheme scheme) {
    super(scheme);
    // Dodaj alerty ze strategii do istniejącej listy
    List<AlertEventType> strategyAlerts = strategy.analyze(this);
    if (strategyAlerts != null) {
      this.alerts.addAll(strategyAlerts);
    }
  }

  void notifyNotifiers(List<IAlertNotifier> notifiers) {
    if (alerts.isEmpty())
      return;

    notifiers.forEach(notifier -> {
      alerts.forEach(alert -> {
        notifier.notify(this.getId(), alert);
      });
    });

    this.alerts.clear();
  }

  @Override
  protected String generateContent(Map<ConfigurationType, Double> data) {
    if (data == null || data.isEmpty()) {
      return "{\"error\": \"No data available\"}";
    }

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    // Grupuj wartości po ConfigurationType i sprawdź alerty
    Map<ConfigurationType, List<Double>> groupedData = new HashMap<>();
    for (Map.Entry<ConfigurationType, Double> entry : data.entrySet()) {
      ConfigurationType type = entry.getKey();
      Double value = entry.getValue();

      groupedData.computeIfAbsent(type, k -> new ArrayList<>()).add(value);

      // Sprawdź czy wartość wykracza poza typowy zakres i utwórz alert
      if (isOutOfTypicalRange(type, value)) {
        AlertEventType alert = getAlertForConfigurationType(type);
        if (alert != null && !this.alerts.contains(alert)) {
          this.alerts.add(alert);
        }
      }
    }

    // Oblicz statystyki dla każdego typu
    Map<ConfigurationType, MetricStatistics> metricsMap = new HashMap<>();
    for (Map.Entry<ConfigurationType, List<Double>> entry : groupedData.entrySet()) {
      MetricStatistics stats = analyzeMetric(entry.getKey(), entry.getValue());
      if (stats != null) {
        metricsMap.put(entry.getKey(), stats);
      }
    }

    if (metricsMap.isEmpty()) {
      return "{\"error\": \"No statistics could be calculated\"}";
    }

    // Generuj zawartość
    Map<String, Object> contentMap = new HashMap<>();
    contentMap.put("metricsCount", metricsMap.size());
    contentMap.put("analysisType", "Multi-Metric Analysis");

    // Dodaj szczegółowe dane dla każdej metryki
    Map<String, Object> metricsData = new HashMap<>();
    for (Map.Entry<ConfigurationType, MetricStatistics> entry : metricsMap.entrySet()) {
      String key = entry.getKey().name().toLowerCase();
      MetricStatistics stats = entry.getValue();

      Map<String, Object> metricMap = new HashMap<>();
      metricMap.put("configurationType", stats.getConfigurationType().name());
      metricMap.put("deviceType", stats.getConfigurationType().getDeviceType().getValue());
      metricMap.put("unit", stats.getUnit());
      metricMap.put("sampleCount", stats.getSampleCount());

      // Statystyki opisowe
      Map<String, Object> descriptiveStats = new HashMap<>();
      descriptiveStats.put("min", round(stats.getMin(), 2));
      descriptiveStats.put("max", round(stats.getMax(), 2));
      descriptiveStats.put("average", round(stats.getAverage(), 2));
      descriptiveStats.put("median", round(stats.getMedian(), 2));
      descriptiveStats.put("range", round(stats.getRange(), 2));
      descriptiveStats.put("standardDeviation", round(stats.getStandardDeviation(), 2));
      metricMap.put("descriptiveStatistics", descriptiveStats);

      // Percentyle
      Map<String, Object> percentiles = new HashMap<>();
      percentiles.put("p25", round(stats.getPercentile25(), 2));
      percentiles.put("p50_median", round(stats.getMedian(), 2));
      percentiles.put("p75", round(stats.getPercentile75(), 2));
      metricMap.put("percentiles", percentiles);

      // Zakresy referencyjne
      Map<String, Object> referenceRanges = new HashMap<>();
      Map<String, Object> typicalRange = new HashMap<>();
      typicalRange.put("min", stats.getTypicalMin());
      typicalRange.put("max", stats.getTypicalMax());
      referenceRanges.put("typical", typicalRange);

      Map<String, Object> optimalRange = new HashMap<>();
      optimalRange.put("min", stats.getOptimalMin());
      optimalRange.put("max", stats.getOptimalMax());
      referenceRanges.put("optimal", optimalRange);
      metricMap.put("referenceRanges", referenceRanges);

      // Analiza zgodności z zakresami
      Map<String, Object> rangeCompliance = new HashMap<>();
      rangeCompliance.put("outOfTypicalRangeCount", stats.getOutOfRangeCount());
      rangeCompliance.put("outOfTypicalRangePercentage", round(stats.getOutOfRangePercentage(), 2));
      rangeCompliance.put("inOptimalRangeCount", stats.getInOptimalRangeCount());
      rangeCompliance.put("inOptimalRangePercentage", round(stats.getInOptimalRangePercentage(), 2));
      metricMap.put("rangeCompliance", rangeCompliance);

      // Ocena jakości
      String qualityAssessment = assessQuality(stats);
      metricMap.put("qualityAssessment", qualityAssessment);

      // Rekomendacje
      List<String> recommendations = generateRecommendations(stats);
      metricMap.put("recommendations", recommendations);

      metricsData.put(key, metricMap);
    }
    contentMap.put("metrics", metricsData);

    // Ogólna ocena
    String overallAssessment = assessOverallQuality(metricsMap);
    contentMap.put("overallAssessment", overallAssessment);

    try {
      return mapper.writeValueAsString(contentMap);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return "{\"error\": \"Failed to generate JSON: " + e.getMessage() + "\"}";
    }
  }

  private double round(double value, int places) {
    double scale = Math.pow(10, places);
    return Math.round(value * scale) / scale;
  }

  /**
   * Mapuje ConfigurationType na odpowiedni AlertEventType
   */
  private AlertEventType getAlertForConfigurationType(ConfigurationType type) {
    if (type == null) {
      return null;
    }

    switch (type) {
      case Temperature:
        return AlertEventType.TemperatureExceedsThreshold;
      case Humidity:
        return AlertEventType.HumidityExceedsThreshold;
      case Pressure:
        return AlertEventType.PressureExceedsThreshold;
      case Power:
        return AlertEventType.PowerExceedsThreshold;
      case Luminosity:
        return AlertEventType.LuminosityExceedsThreshold;
      case CO2Level:
        return AlertEventType.CO2LevelExceedsThreshold;
      case NoiseLevel:
        return AlertEventType.NoiseLevelExceedsThreshold;
      default:
        return null;
    }
  }

  /**
   * Ocenia jakość pomiarów na podstawie statystyk
   */
  private String assessQuality(MetricStatistics stats) {
    double optimalPercentage = stats.getInOptimalRangePercentage();
    double outOfRangePercentage = stats.getOutOfRangePercentage();

    if (optimalPercentage >= 90) {
      return "EXCELLENT - Większość pomiarów w optymalnym zakresie";
    } else if (optimalPercentage >= 70) {
      return "GOOD - Pomiary w akceptowalnym zakresie";
    } else if (optimalPercentage >= 50) {
      return "FAIR - Pomiary wymagają uwagi";
    } else if (outOfRangePercentage > 30) {
      return "POOR - Wiele pomiarów poza typowym zakresem";
    } else {
      return "NEEDS_ATTENTION - Pomiary wymagają natychmiastowej uwagi";
    }
  }

  /**
   * Generuje rekomendacje na podstawie statystyk
   */
  private List<String> generateRecommendations(MetricStatistics stats) {
    List<String> recommendations = new ArrayList<>();
    ConfigurationType type = stats.getConfigurationType();

    // Sprawdź czy średnia jest w optymalnym zakresie
    if (!isInOptimalRange(type, stats.getAverage())) {
      if (stats.getAverage() < stats.getOptimalMin()) {
        recommendations.add("Średnia wartość poniżej optymalnego zakresu - rozważ zwiększenie");
      } else if (stats.getAverage() > stats.getOptimalMax()) {
        recommendations.add("Średnia wartość powyżej optymalnego zakresu - rozważ zmniejszenie");
      }
    }

    // Sprawdź odchylenie standardowe
    double coefficientOfVariation = (stats.getStandardDeviation() / Math.abs(stats.getAverage())) * 100;
    if (coefficientOfVariation > 20) {
      recommendations.add("Wysoka zmienność pomiarów (CV=" + round(coefficientOfVariation, 1) +
          "%) - rozważ stabilizację warunków");
    }

    // Sprawdź procent wartości poza zakresem
    if (stats.getOutOfRangePercentage() > 10) {
      recommendations.add(stats.getOutOfRangePercentage() + "% pomiarów poza typowym zakresem - " +
          "sprawdź kalibrację urządzeń");
    }

    // Sprawdź procent wartości w optymalnym zakresie
    if (stats.getInOptimalRangePercentage() < 70) {
      recommendations.add("Tylko " + round(stats.getInOptimalRangePercentage(), 1) +
          "% pomiarów w optymalnym zakresie - rozważ optymalizację");
    }

    // Specyficzne rekomendacje dla typu konfiguracji
    switch (type) {
      case Temperature:
        if (stats.getAverage() < 20) {
          recommendations.add("Niska średnia temperatura - rozważ zwiększenie ogrzewania");
        } else if (stats.getAverage() > 25) {
          recommendations.add("Wysoka średnia temperatura - rozważ zwiększenie chłodzenia");
        }
        break;

      case Humidity:
        if (stats.getAverage() < 40) {
          recommendations.add("Niska wilgotność - rozważ użycie nawilżacza");
        } else if (stats.getAverage() > 60) {
          recommendations.add("Wysoka wilgotność - rozważ użycie osuszacza");
        }
        break;

      case CO2Level:
        if (stats.getAverage() > 1000) {
          recommendations.add("Wysoki poziom CO2 - zwiększ wentylację");
        }
        break;

      case Power:
        if (stats.getAverage() > 1000) {
          recommendations.add("Wysokie zużycie energii - rozważ optymalizację");
        }
        break;

      default:
        break;
    }

    if (recommendations.isEmpty()) {
      recommendations.add("Wszystkie parametry w normie - brak dodatkowych rekomendacji");
    }

    return recommendations;
  }

  /**
   * Ocenia ogólną jakość wszystkich metryk
   */
  private String assessOverallQuality(Map<ConfigurationType, MetricStatistics> metricsMap) {
    int excellentCount = 0;
    int goodCount = 0;
    int fairCount = 0;
    int poorCount = 0;

    for (MetricStatistics stats : metricsMap.values()) {
      String assessment = assessQuality(stats);
      if (assessment.startsWith("EXCELLENT")) {
        excellentCount++;
      } else if (assessment.startsWith("GOOD")) {
        goodCount++;
      } else if (assessment.startsWith("FAIR")) {
        fairCount++;
      } else {
        poorCount++;
      }
    }

    int total = metricsMap.size();
    if (excellentCount == total) {
      return "EXCELLENT - Wszystkie metryki w doskonałym stanie";
    } else if (excellentCount + goodCount >= total * 0.8) {
      return "GOOD - Większość metryk w dobrym stanie";
    } else if (poorCount > total * 0.5) {
      return "POOR - Wiele metryk wymaga natychmiastowej uwagi";
    } else {
      return "FAIR - System wymaga optymalizacji";
    }
  }

  @Override
  public String getDocumentType() {
    return "Analiza";
  }

  /**
   * Zwraca jednostkę dla danego typu konfiguracji
   */
  private static String getUnitForConfigurationType(ConfigurationType configurationType) {
    if (configurationType == null) {
      return "";
    }

    switch (configurationType) {
      case Temperature:
        return "°C";
      case Humidity:
        return "%";
      case Pressure:
        return "hPa";
      case Power:
        return "W";
      case Luminosity:
        return "%";
      case CO2Level:
        return "ppm";
      case NoiseLevel:
        return "dB";
      default:
        return "";
    }
  }

  /**
   * Zwraca typowe zakresy wartości dla danego typu konfiguracji (hard-coded)
   */
  private static Map<String, Double> getTypicalRanges(ConfigurationType configurationType) {
    Map<String, Double> ranges = new HashMap<>();

    if (configurationType == null) {
      return ranges;
    }

    switch (configurationType) {
      case Temperature:
        ranges.put("min_typical", 15.0);
        ranges.put("max_typical", 30.0);
        ranges.put("optimal_min", 18.0);
        ranges.put("optimal_max", 24.0);
        break;

      case Humidity:
        ranges.put("min_typical", 30.0);
        ranges.put("max_typical", 70.0);
        ranges.put("optimal_min", 40.0);
        ranges.put("optimal_max", 60.0);
        break;

      case Pressure:
        ranges.put("min_typical", 980.0);
        ranges.put("max_typical", 1030.0);
        ranges.put("optimal_min", 1000.0);
        ranges.put("optimal_max", 1020.0);
        break;

      case Power:
        ranges.put("min_typical", 0.0);
        ranges.put("max_typical", 3000.0);
        ranges.put("optimal_min", 0.0);
        ranges.put("optimal_max", 1500.0);
        break;

      case Luminosity:
        ranges.put("min_typical", 0.0);
        ranges.put("max_typical", 100.0);
        ranges.put("optimal_min", 40.0);
        ranges.put("optimal_max", 80.0);
        break;

      case CO2Level:
        ranges.put("min_typical", 400.0);
        ranges.put("max_typical", 2000.0);
        ranges.put("optimal_min", 400.0);
        ranges.put("optimal_max", 1000.0);
        break;

      case NoiseLevel:
        ranges.put("min_typical", 30.0);
        ranges.put("max_typical", 90.0);
        ranges.put("optimal_min", 30.0);
        ranges.put("optimal_max", 55.0);
        break;

      default:
        break;
    }

    return ranges;
  }

  /**
   * Sprawdza, czy wartość jest poza typowym zakresem
   */
  private static boolean isOutOfTypicalRange(ConfigurationType configurationType, double value) {
    Map<String, Double> ranges = getTypicalRanges(configurationType);
    if (ranges.isEmpty()) {
      return false;
    }

    double min = ranges.getOrDefault("min_typical", Double.MIN_VALUE);
    double max = ranges.getOrDefault("max_typical", Double.MAX_VALUE);

    return value < min || value > max;
  }

  /**
   * Sprawdza, czy wartość jest w optymalnym zakresie
   */
  private static boolean isInOptimalRange(ConfigurationType configurationType, double value) {
    Map<String, Double> ranges = getTypicalRanges(configurationType);
    if (ranges.isEmpty()) {
      return true;
    }

    double min = ranges.getOrDefault("optimal_min", Double.MIN_VALUE);
    double max = ranges.getOrDefault("optimal_max", Double.MAX_VALUE);

    return value >= min && value <= max;
  }

  /**
   * Klasa wewnętrzna do przechowywania statystyk dla generowania contentu
   */
  private static class MetricStatistics {
    private double min;
    private double max;
    private double average;
    private double median;
    private String unit;
    private ConfigurationType configurationType;
    private double range;
    private double standardDeviation;
    private double percentile25;
    private double percentile75;
    private int sampleCount;
    private int outOfRangeCount;
    private int inOptimalRangeCount;
    private double typicalMin;
    private double typicalMax;
    private double optimalMin;
    private double optimalMax;

    public MetricStatistics(double min, double max, double average, double median, String unit,
        ConfigurationType configurationType, double range, double standardDeviation,
        double percentile25, double percentile75, int sampleCount,
        int outOfRangeCount, int inOptimalRangeCount,
        double typicalMin, double typicalMax, double optimalMin, double optimalMax) {
      this.min = min;
      this.max = max;
      this.average = average;
      this.median = median;
      this.unit = unit;
      this.configurationType = configurationType;
      this.range = range;
      this.standardDeviation = standardDeviation;
      this.percentile25 = percentile25;
      this.percentile75 = percentile75;
      this.sampleCount = sampleCount;
      this.outOfRangeCount = outOfRangeCount;
      this.inOptimalRangeCount = inOptimalRangeCount;
      this.typicalMin = typicalMin;
      this.typicalMax = typicalMax;
      this.optimalMin = optimalMin;
      this.optimalMax = optimalMax;
    }

    public double getMin() {
      return min;
    }

    public double getMax() {
      return max;
    }

    public double getAverage() {
      return average;
    }

    public double getMedian() {
      return median;
    }

    public String getUnit() {
      return unit;
    }

    public ConfigurationType getConfigurationType() {
      return configurationType;
    }

    public double getRange() {
      return range;
    }

    public double getStandardDeviation() {
      return standardDeviation;
    }

    public double getPercentile25() {
      return percentile25;
    }

    public double getPercentile75() {
      return percentile75;
    }

    public int getSampleCount() {
      return sampleCount;
    }

    public int getOutOfRangeCount() {
      return outOfRangeCount;
    }

    public int getInOptimalRangeCount() {
      return inOptimalRangeCount;
    }

    public double getTypicalMin() {
      return typicalMin;
    }

    public double getTypicalMax() {
      return typicalMax;
    }

    public double getOptimalMin() {
      return optimalMin;
    }

    public double getOptimalMax() {
      return optimalMax;
    }

    public double getOutOfRangePercentage() {
      return sampleCount > 0 ? (outOfRangeCount * 100.0 / sampleCount) : 0.0;
    }

    public double getInOptimalRangePercentage() {
      return sampleCount > 0 ? (inOptimalRangeCount * 100.0 / sampleCount) : 0.0;
    }

    @Override
    public String toString() {
      return String.format(
          "MetricStatistics{type=%s, count=%d, min=%.2f%s, max=%.2f%s, avg=%.2f%s, median=%.2f%s, " +
              "range=%.2f%s, stdDev=%.2f%s, p25=%.2f%s, p75=%.2f%s, " +
              "outOfRange=%d (%.1f%%), inOptimal=%d (%.1f%%)}",
          configurationType.name(), sampleCount,
          min, unit, max, unit,
          average, unit, median, unit,
          range, unit, standardDeviation, unit,
          percentile25, unit, percentile75, unit,
          outOfRangeCount, getOutOfRangePercentage(),
          inOptimalRangeCount, getInOptimalRangePercentage());
    }

    public Map<String, Object> toMap() {
      Map<String, Object> map = new HashMap<>();
      map.put("configurationType", configurationType.name());
      map.put("unit", unit);
      map.put("sampleCount", sampleCount);
      map.put("min", min);
      map.put("max", max);
      map.put("average", average);
      map.put("median", median);
      map.put("range", range);
      map.put("standardDeviation", standardDeviation);
      map.put("percentile25", percentile25);
      map.put("percentile75", percentile75);
      map.put("outOfRangeCount", outOfRangeCount);
      map.put("outOfRangePercentage", getOutOfRangePercentage());
      map.put("inOptimalRangeCount", inOptimalRangeCount);
      map.put("inOptimalRangePercentage", getInOptimalRangePercentage());
      map.put("typicalMin", typicalMin);
      map.put("typicalMax", typicalMax);
      map.put("optimalMin", optimalMin);
      map.put("optimalMax", optimalMax);
      return map;
    }
  }

  /**
   * Oblicza statystyki z dodatkowymi informacjami (metoda prywatna dla
   * generowania contentu)
   */
  private static MetricStatistics analyzeMetric(ConfigurationType configurationType, List<Double> values) {
    if (values == null || values.isEmpty()) {
      return null;
    }

    List<Double> sortedValues = new ArrayList<>(values);
    Collections.sort(sortedValues);

    int size = sortedValues.size();
    double min = sortedValues.get(0);
    double max = sortedValues.get(size - 1);
    double range = max - min;

    // Średnia
    double sum = 0;
    for (double value : values) {
      sum += value;
    }
    double average = sum / size;

    // Mediana
    double median;
    if (size % 2 == 0) {
      median = (sortedValues.get(size / 2 - 1) + sortedValues.get(size / 2)) / 2.0;
    } else {
      median = sortedValues.get(size / 2);
    }

    // Odchylenie standardowe
    double sumSquaredDiff = 0;
    for (double value : values) {
      double diff = value - average;
      sumSquaredDiff += diff * diff;
    }
    double variance = sumSquaredDiff / size;
    double standardDeviation = Math.sqrt(variance);

    // Percentyle
    int p25Index = (int) Math.ceil(size * 0.25) - 1;
    int p75Index = (int) Math.ceil(size * 0.75) - 1;
    p25Index = Math.max(0, Math.min(p25Index, size - 1));
    p75Index = Math.max(0, Math.min(p75Index, size - 1));
    double percentile25 = sortedValues.get(p25Index);
    double percentile75 = sortedValues.get(p75Index);

    // Zliczanie wartości poza zakresem i w optymalnym zakresie
    int outOfRangeCount = 0;
    int inOptimalRangeCount = 0;
    for (double value : values) {
      if (isOutOfTypicalRange(configurationType, value)) {
        outOfRangeCount++;
      }
      if (isInOptimalRange(configurationType, value)) {
        inOptimalRangeCount++;
      }
    }

    String unit = getUnitForConfigurationType(configurationType);
    Map<String, Double> ranges = getTypicalRanges(configurationType);

    double typicalMin = ranges.getOrDefault("min_typical", 0.0);
    double typicalMax = ranges.getOrDefault("max_typical", 0.0);
    double optimalMin = ranges.getOrDefault("optimal_min", 0.0);
    double optimalMax = ranges.getOrDefault("optimal_max", 0.0);

    return new MetricStatistics(
        min, max, average, median, unit, configurationType,
        range, standardDeviation, percentile25, percentile75,
        size, outOfRangeCount, inOptimalRangeCount,
        typicalMin, typicalMax, optimalMin, optimalMax);
  }

}
