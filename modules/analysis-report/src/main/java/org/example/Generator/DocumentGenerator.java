package org.example.Generator;

import org.example.Documents.DocumentScheme;
import org.example.Documents.IDocument;

import java.time.Period;
import java.util.List;

public class DocumentGenerator {
  private DocumentGeneratorID ID;
  private List<Pair<DocumentScheme, Period>> documentsConfig;

  boolean initDocument() {
    return true;
  }

  boolean addDocumentConfig(DocumentScheme scheme, Period period) {
    throw new UnsupportedOperationException("Unimplemented method 'addDocumentConfig'");
  }

  boolean removeDocumentConfig(DocumentScheme scheme, Period period) {
    throw new UnsupportedOperationException("Unimplemented method 'removeDocumentConfig'");
  }

  List<DocumentScheme> getDocumentSchemes() {
    throw new UnsupportedOperationException("Unimplemented method 'getDocumentSchemes'");
  }

  List<DocumentScheme> getPeriods() {
    throw new UnsupportedOperationException("Unimplemented method 'getDocumentSchemes'");
  }

  boolean clearDocumentConfigs() {
    throw new UnsupportedOperationException("Unimplemented method 'clearDocumentConfigs'");
  }

  class Pair<K, V> {
    public final K first;
    public final V second;

    public Pair(K first, V second) {
      this.first = first;
      this.second = second;
    }
  }
}
