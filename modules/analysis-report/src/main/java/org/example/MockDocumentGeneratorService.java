package org.example;

import java.time.Period;
import java.util.List;

public class MockDocumentGeneratorService implements IDocumentGeneratorService {

  // TUTAJ JAKIES POLE CO BEDZIE PRZECHOWYWAŁO LABELE ETC

  @Override
  public List<DocumentGenerator> build(DocumentGenerator.Builder documentGeneratorBuilder) {
    var documentBuilder = AnalysisReportAPI.newReportScheme();
    var metrics = AnalysisReportAPI.getAvailableMetrics();

    documentBuilder.includeMetrics(metrics);
    documentGeneratorBuilder.addDocumentConfig(documentBuilder, Period.ofDays(1));

    return List.of(documentGeneratorBuilder.build());
  }
}
