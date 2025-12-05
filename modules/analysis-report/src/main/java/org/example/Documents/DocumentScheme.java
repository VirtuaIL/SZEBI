package org.example.Documents;

import java.util.HashMap;

public class DocumentScheme {
  HashMap<Object, IDocument> configurations;

  boolean configure(String optionName, Object option) {
    throw new UnsupportedOperationException("Unimplemented method 'build'");
  }

  IDocument build() {
    throw new UnsupportedOperationException("Unimplemented method 'build'");
  }

  HashMap<String, Object> getAvailableConfigurations() {
    throw new UnsupportedOperationException("Unimplemented method 'build'");
  }

  // HashMap<Object, IDocument> getAvailableStrategies() {
  // throw new UnsupportedOperationException("Unimplemented method 'build'");
  // }
}
