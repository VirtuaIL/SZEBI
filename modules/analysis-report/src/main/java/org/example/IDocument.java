package org.example;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

interface IDocument {
  public LocalDateTime getCreationDate();

  public String getId();

  public String getContent();

  public LocalDateTime getDateFrom();

  public LocalDateTime getDateTo();

  public String getDocumentType();

  abstract class Document implements IDocument {

    private String id;
    private String content;

    private LocalDateTime creationDate;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;

    protected Document(Scheme builder) {
      this.id = builder.id;
      this.content = generateContent(
          AnalysisReportAPI.getAquisitionProxy().getLabelAndValuesFor(builder.metrics));
      this.creationDate = LocalDateTime.now();
      this.dateFrom = builder.from;
      this.dateTo = builder.to;
    }

    public String getId() {
      return id;
    }

    public String getContent() {
      return content;
    }

    public LocalDateTime getCreationDate() {
      return creationDate;
    }

    public LocalDateTime getDateFrom() {
      return dateFrom;
    }

    public LocalDateTime getDateTo() {
      return dateTo;
    }

    abstract protected String generateContent(Map<String, Double> data);

    @Override
    public String toString() {
      return "Document{" +
          "id=" + id +
          ", content='" + content + '\'' +
          ", creationDate=" + creationDate +
          ", dateFrom=" + dateFrom +
          ", dateTo=" + dateTo +
          '}';
    }

  }

  public static Scheme<Report> reportBuilder() {
    return new IDocument.Scheme<>(Report::new);
  }

  public static Scheme<Analysis> analysisBuilder() {
    return new IDocument.Scheme<>(Analysis::new);
  }

  public static class Scheme<T extends Document> {
    private Function<Scheme<?>, ? extends Document> constructor;

    private String generateUniqueId() {
      return UUID.randomUUID().toString();
    }

    public Scheme<Report> withReport() {
      this.constructor = Report::new;
      return (Scheme<Report>) this;
    }

    public Scheme<Analysis> withAnalysis() {
      this.constructor = Analysis::new;
      return (Scheme<Analysis>) this;
    }

    private String id;

    private HashSet<String> metrics = new HashSet<>();
    private LocalDateTime from = LocalDateTime.MIN;
    private LocalDateTime to = LocalDateTime.MAX;

    public Scheme(Function<Scheme<?>, ? extends Document> constructor) {
      this.id = generateUniqueId();
      this.constructor = constructor;
    }

    public Scheme includeMetrics(String... confs) {
      this.metrics.addAll(List.of(confs));
      return this;
    }

    public Scheme includeMetrics(List<String> confs) {
      this.metrics.addAll(confs);
      return this;
    }

    public Scheme includeMetrics(Set<String> confs) {
      this.metrics.addAll(confs);
      return this;
    }

    // public Scheme includeMetrics(Collection<String> confs) {
    // this.metrics.addAll(confs);
    // return this;
    // }

    public Scheme excludeMetrics(String... confs) {
      this.metrics.removeAll(List.of(confs));
      return this;
    }

    public Scheme excludeMetrics(Collection<String> confs) {
      this.metrics.removeAll(confs);
      return this;
    }

    public Scheme setFrom(LocalDateTime from) {
      this.from = from;
      return this;
    }

    public Scheme setTo(LocalDateTime to) {
      this.to = to;
      return this;
    }

    Document build() {
      if (constructor == null) {
        throw new IllegalStateException("No document type specified");
      }
      return constructor.apply(this);
    }
  }
}
