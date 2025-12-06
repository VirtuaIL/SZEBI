package org.example;

import java.time.Period;
import java.util.List;

import org.example.Documents.IDocument;
import org.example.DocumentGenerator.Builder;

public class MockDocumentGeneratorService implements IDocumentGeneratorService {

  // TUTAJ JAKIES POLE CO BEDZIE PRZECHOWYWAŁO LABELE ETC

  @Override
  public List<DocumentGenerator> build(Builder builder) {
    // Można też tak
    var allAvailableConf = AnalysisReportAPI.aquisitionService.getLabelValues();

    // a można tak
    var allMetrics = AnalysisReportAPI.aquisitionService.getLabels();
    allMetrics.remove("GÓWNO");
    var allConfForSomeMetrics = AnalysisReportAPI.aquisitionService.getLabelAndValuesFor(allMetrics);

    var documentBuilder = new IDocument.Builder(allConfForSomeMetrics);
    builder.addDocumentConfig(documentBuilder, Period.ofDays(1));

    return List.of(builder.build());
  }
}
