package org.example;

import java.util.List;

public interface IDocumentGeneratorService {
  List<DocumentGenerator> build(DocumentGenerator.Builder builder);
}
