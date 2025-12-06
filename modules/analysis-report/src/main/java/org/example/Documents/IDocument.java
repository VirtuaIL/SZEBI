
package org.example.Documents;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IDocument {
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
      this.content = generateContent(builder.configurations.toString());
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

    // ----------------------------------------------------
    // NESTED BUILDER CLASS
    // ----------------------------------------------------
  }

  public static class Builder {
    private static String generateUniqueId() {
      return UUID.randomUUID().toString();
    }

    private String id;

    private Map<String, List<Double>> configurations;
    private LocalDateTime from = LocalDateTime.MIN;
    private LocalDateTime to = LocalDateTime.MAX;

    public Builder(Map<String, List<Double>> configurations) {
      this.id = generateUniqueId();
      this.configurations = configurations;
    }

    public Builder setConfigurations(Map<String, List<Double>> configurations) {
      this.configurations = configurations;
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

    public Report buildReport() {
      return new Report(this);
    }

    public Analysis buildAnalysis() {
      return new Analysis(this);
    }
  }
}
