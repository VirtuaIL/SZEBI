package org.example;

import java.time.Period;
import java.util.List;

import org.example.Documents.IDocument;

public class MockDocumentGeneratorService implements IDocumentGeneratorService {

  // TUTAJ JAKIES POLE CO BEDZIE PRZECHOWYWAŁO LABELE ETC

  @Override
  public List<DocumentGenerator> build(DocumentGenerator.Builder builder) {
    var documentBuilder = IDocument.buildReport();
    var metrics = IDocument.getAvailableMetrics();

    documentBuilder.includeMetrics(metrics);
    builder.addDocumentConfig(documentBuilder, Period.ofDays(1));

    return List.of(builder.build());
  }
}
