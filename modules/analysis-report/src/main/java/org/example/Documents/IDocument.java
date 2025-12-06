package org.example.Documents;

import java.time.LocalDateTime;

public interface IDocument<T extends Document> {
  T getThis();

  default public LocalDateTime getCreationDate() {
    return getThis().getCreationDate();
  }

  default public Integer getId() {
    return getThis().getId();
  }

  default public void setId(Integer id) {
    this.getThis().setId(id);
  }

  default public String getContent() {
    return getThis().getContent();
  }

  default public void setContent(String content) {
    this.getThis().setContent(content);
  }

  default public LocalDateTime getDateFrom() {
    return getThis().getDateFrom();
  }

  default public void setDateFrom(LocalDateTime dateFrom) {
    this.getThis().setDateFrom(dateFrom);
  }

  default public LocalDateTime getDateTo() {
    return getThis().getDateTo();
  }

  default public void setDateTo(LocalDateTime dateTo) {
    this.getThis().setDateTo(dateTo);
  }

  public String getDocumentType();

}
