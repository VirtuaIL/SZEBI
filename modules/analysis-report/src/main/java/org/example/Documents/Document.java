package org.example.Documents;

import java.time.LocalDateTime;

public sealed class Document permits Report, Analysis {
  private Integer id;
  private String content;

  private LocalDateTime creationDate;
  private LocalDateTime dateFrom;
  private LocalDateTime dateTo;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public LocalDateTime getDateFrom() {
    return dateFrom;
  }

  public void setDateFrom(LocalDateTime dateFrom) {
    this.dateFrom = dateFrom;
  }

  public LocalDateTime getDateTo() {
    return dateTo;
  }

  public void setDateTo(LocalDateTime dateTo) {
    this.dateTo = dateTo;
  }

  public LocalDateTime getCreationDate() {
    return creationDate;
  }

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
