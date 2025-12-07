package org.example;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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

  public abstract sealed class Document implements IDocument permits Report, Analysis {

    private String id;
    private String content;

    private LocalDateTime creationDate;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;

    protected Document(Builder builder) {
      this.id = builder.id;
      this.content = generateContent(
          AquisitionProxy.singleton.getLabelAndValuesFor(builder.metrics).toString());
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

    abstract protected String generateContent(String data);

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

  public static Builder<Report> reportBuilder() {
    return new IDocument.Builder<>(Report::new);
  }

  public static Builder<Analysis> analysisBuilder() {
    return new IDocument.Builder<>(Analysis::new);
  }

  public static class Builder<T extends Document> {
    private Function<Builder<?>, ? extends Document> constructor;

    private String generateUniqueId() {
      return UUID.randomUUID().toString();
    }

    public Builder<Report> withReport() {
      this.constructor = Report::new;
      return (Builder<Report>) this;
    }

    public Builder<Analysis> withAnalysis() {
      this.constructor = Analysis::new;
      return (Builder<Analysis>) this;
    }

    private String id;

    private Set<String> metrics = new HashSet<>();
    private LocalDateTime from = LocalDateTime.MIN;
    private LocalDateTime to = LocalDateTime.MAX;

    public Builder(Function<Builder<?>, ? extends Document> constructor) {
      this.id = generateUniqueId();
      this.constructor = constructor;
    }

    public Builder includeMetrics(String... confs) {
      this.metrics.addAll(List.of(confs));
      return this;
    }

    public Builder includeMetrics(Collection<String> confs) {
      this.metrics.addAll(confs);
      return this;
    }

    public Builder excludeMetrics(String... confs) {
      this.metrics.removeAll(List.of(confs));
      return this;
    }

    public Builder exludeMetrics(Collection<String> confs) {
      this.metrics.removeAll(confs);
      return this;
    }

    public Builder setFrom(LocalDateTime from) {
      this.from = from;
      return this;
    }

    public Builder setTo(LocalDateTime to) {
      this.to = to;
      return this;
    }

    public Document build() {
      if (constructor == null) {
        throw new IllegalStateException("No document type specified");
      }
      return constructor.apply(this);
    }
  }
}
