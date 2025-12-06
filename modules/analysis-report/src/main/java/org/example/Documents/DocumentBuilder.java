package org.example.Documents;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentBuilder {
  private Map<String, List<Double>> configurations;
  private LocalDateTime from;
  private LocalDateTime to;

  public DocumentBuilder(HashMap<String, List<Double>> configurations) {
    this.configurations = configurations;
    this.from = LocalDateTime.MIN;
    this.to = LocalDateTime.MAX;
  }

  public Map<String, List<Double>> getConfigurations() {
    return configurations;
  }

  public DocumentBuilder setConfigurations(Map<String, List<Double>> configurations) {
    this.configurations = configurations;
    return this;
  }

  public LocalDateTime getFrom() {
    return from;
  }

  public DocumentBuilder setFrom(LocalDateTime from) {
    this.from = from;
    return this;
  }

  public LocalDateTime getTo() {
    return to;
  }

  public DocumentBuilder setTo(LocalDateTime to) {
    this.to = to;
    return this;
  }

  // public void configure(ConfigurationType optionName, Number option) {
  // configurations.put(optionName, option);
  // }

  // HashMap<Object, IDocument> getAvailableStrategies() {
  // throw new UnsupportedOperationException("Unimplemented method 'build'");
  // }
}
