package org.example.Documents;

import java.time.LocalDateTime;
import java.util.Set;

public class DocumentScheme {
  private Set<String> configurations;
  private LocalDateTime from;
  private LocalDateTime to;

  DocumentScheme(Set<String> configurations, LocalDateTime from, LocalDateTime to) {
    this.configurations = configurations;
    this.from = from;
    this.to = to;
  }

  public Set<String> getConfigurations() {
    return configurations;
  }

  public void setConfigurations(Set<String> configurations) {
    this.configurations = configurations;
  }

  public LocalDateTime getFrom() {
    return from;
  }

  public void setFrom(LocalDateTime from) {
    this.from = from;
  }

  public LocalDateTime getTo() {
    return to;
  }

  public void setTo(LocalDateTime to) {
    this.to = to;
  }

  // public void configure(ConfigurationType optionName, Number option) {
  // configurations.put(optionName, option);
  // }

  // HashMap<Object, IDocument> getAvailableStrategies() {
  // throw new UnsupportedOperationException("Unimplemented method 'build'");
  // }
}
