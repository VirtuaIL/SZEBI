package org.example.Generator;

import org.example.IDataPersistenceService;
import org.example.Documents.DocumentScheme;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DocumentGenerator {
  interface PeriodFunc {
    long run(Period p);
  }

  private final ScheduledExecutorService scheduler;
  private final String id;
  private final Map<DocumentScheme, List<Period>> documentsConfig;
  private final LocalDate creationTime;
  private final IDataPersistenceService database;

  private DocumentGenerator(Builder builder) {
    this.id = generateUniqueId();
    this.scheduler = builder.scheduler != null ? builder.scheduler : Executors.newScheduledThreadPool(1);
    this.documentsConfig = new HashMap<>(builder.documentsConfig);
    this.creationTime = LocalDate.now();
    this.database = builder.database;
  }

  private static String generateUniqueId() {
    return UUID.randomUUID().toString();
  }

  public static class Builder {
    private ScheduledExecutorService scheduler;
    private final Map<DocumentScheme, List<Period>> documentsConfig = new HashMap<>();
    private IDataPersistenceService database;

    public Builder id(String id) {
      return this;
    }

    public Builder addDocumentConfig(DocumentScheme scheme, Period period) {
      this.documentsConfig.computeIfAbsent(scheme, k -> new ArrayList<>()).add(period);
      return this;
    }

    public Builder addDocumentConfigs(DocumentScheme scheme, List<Period> periods) {
      this.documentsConfig.computeIfAbsent(scheme, k -> new ArrayList<>()).addAll(periods);
      return this;
    }

    public Builder addDocumentConfigs(DocumentScheme scheme, Period... periods) {
      if (periods != null && periods.length > 0) {
        this.documentsConfig.computeIfAbsent(scheme, k -> new ArrayList<>()).addAll(Arrays.asList(periods));
      }
      return this;
    }

    public Builder addDocumentConfigs(Map<DocumentScheme, List<Period>> configs) {
      if (configs != null) {
        for (Map.Entry<DocumentScheme, List<Period>> entry : configs.entrySet()) {
          this.documentsConfig.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
              .addAll(entry.getValue());
        }
      }
      return this;
    }

    public DocumentGenerator build() {
      return new DocumentGenerator(this);
    }

    public Builder database(IDataPersistenceService dataService) {
      this.database = dataService;
      return this;
    }
  }

  public static Builder builder(IDataPersistenceService dataService) {
    return new Builder().database(dataService);
  }

  public static Builder builder(String customId) {
    return new Builder().id(customId);
  }

  boolean initDocumentGenerator() {
    PeriodFunc periodToMillis = (p) -> {
      LocalDate now = LocalDate.now();
      LocalDate then = now.plus(p);

      long days = ChronoUnit.DAYS.between(now, then);
      return Duration.ofDays(days).toMillis();
    };

    for (var entry : documentsConfig.entrySet()) {
      DocumentScheme scheme = entry.getKey();

      for (Period period : entry.getValue()) {
        long delayMs = periodToMillis.run(period);

        scheduler.schedule(() -> {
          try {
            scheme.build();
          } catch (Exception e) {
            System.err.println("Error building document scheme: " + scheme);
            e.printStackTrace();
          }
        }, delayMs, TimeUnit.MILLISECONDS);
      }
    }

    return true;
  }

  List<DocumentScheme> getDocumentSchemes() {
    return Collections.unmodifiableList(new ArrayList<>(this.documentsConfig.keySet()));
  }

  public Map<DocumentScheme, List<Period>> getDocumentConfigs() {
    return Collections.unmodifiableMap(documentsConfig);
  }

  void clearDocumentConfigs() {
    this.documentsConfig.clear();
  }

  public String getId() {
    return id;
  }

  public void shutdown() {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdown();
      try {
        if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
          scheduler.shutdownNow();
        }
      } catch (InterruptedException e) {
        scheduler.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public String toString() {
    return "DocumentGenerator{" +
        "id='" + id + '\'' +
        ", schemeCount=" + documentsConfig.size() +
        ", totalPeriods=" + documentsConfig.values().stream().mapToInt(List::size).sum() +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    DocumentGenerator that = (DocumentGenerator) o;
    return id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  // Inner Pair class
  public static class Pair<K, V> {
    public final K first;
    public final V second;

    public Pair(K first, V second) {
      this.first = first;
      this.second = second;
    }

    @Override
    public String toString() {
      return "Pair{" + first + "=" + second + "}";
    }

    public static <K, V> Pair<K, V> of(K first, V second) {
      return new Pair<>(first, second);
    }
  }
}
