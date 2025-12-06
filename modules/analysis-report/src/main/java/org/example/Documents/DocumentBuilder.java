package org.example.Documents;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentBuilder {
  private Map<String, List<Double>> configurations;
  private LocalDateTime from;
  private LocalDateTime to;

  DocumentBuilder(HashMap<String, List<Double>> configurations, LocalDateTime from, LocalDateTime to) {
    this.configurations = configurations;
    this.from = from;
    this.to = to;
  }

  public Map<String, List<Double>> getConfigurations() {
    return configurations;
  }

  public void setConfigurations(Map<String, List<Double>> configurations) {
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
