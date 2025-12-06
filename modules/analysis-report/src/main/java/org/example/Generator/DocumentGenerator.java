package org.example.Generator;

import org.example.Documents.DocumentScheme;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DocumentGenerator {
  interface PeriodFunc {
    long run(Period p);
  }

  private final ScheduledExecutorService scheduler;
  private final String id;
  private final Map<DocumentScheme, List<Period>> documentsConfig;

  private static final AtomicInteger idCounter = new AtomicInteger(0);

  private DocumentGenerator(Builder builder) {
    this.id = builder.id != null ? builder.id : generateUniqueId();
    this.scheduler = builder.scheduler != null ? builder.scheduler : Executors.newScheduledThreadPool(1);
    this.documentsConfig = new HashMap<>(builder.documentsConfig);
  }

  private static String generateUniqueId() {
    return UUID.randomUUID().toString();
  }

  private static String generateSequentialId() {
    return "DOC_GEN_" + System.currentTimeMillis() + "_" + idCounter.getAndIncrement();
  }

  private static String generateReadableId() {
    return "Generator-" + LocalDate.now().toString() + "-" + idCounter.getAndIncrement();
  }

  public static DocumentGenerator create() {
    return builder().build();
  }

  public static class Builder {
    private String id;
    private ScheduledExecutorService scheduler;
    private final Map<DocumentScheme, List<Period>> documentsConfig = new HashMap<>();

    public Builder id(String id) {
      this.id = id;
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
      if (id == null) {
        id = generateUniqueId();
      }
      return new DocumentGenerator(this);
    }
  }

  // Static factory methods
  public static Builder builder() {
    return new Builder();
  }

  public static Builder builder(String customId) {
    return new Builder().id(customId);
  }

  // Alternative factory methods for different ID types
  public static DocumentGenerator createWithSequentialId() {
    return new Builder().id(generateSequentialId()).build();
  }

  public static DocumentGenerator createWithReadableId() {
    return new Builder().id(generateReadableId()).build();
  }

  // Your existing methods
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

  // Additional helper methods to modify the config (if needed)
  boolean addDocumentConfig(DocumentScheme scheme, Period period) {
    this.documentsConfig.computeIfAbsent(scheme, k -> new ArrayList<>()).add(period);
    return true;
  }

  boolean removeDocumentConfig(DocumentScheme scheme, Period period) {
    List<Period> periods = this.documentsConfig.get(scheme);
    if (periods != null) {
      return periods.remove(period);
    }
    return false;
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

  public ScheduledExecutorService getScheduler() {
    return scheduler;
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
