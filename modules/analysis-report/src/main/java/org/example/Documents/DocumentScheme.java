package org.example.Documents;

import java.time.LocalDateTime;
import java.util.HashMap;

public class DocumentScheme {
  private HashMap<ConfigurationType, Number> configurations;
  private LocalDateTime from;
  private LocalDateTime to;

  // public void configure(ConfigurationType optionName, Number option) {
  // configurations.put(optionName, option);
  // }

  // HashMap<Object, IDocument> getAvailableStrategies() {
  // throw new UnsupportedOperationException("Unimplemented method 'build'");
  // }
}
