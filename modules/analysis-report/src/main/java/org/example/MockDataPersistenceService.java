package org.example;

import java.util.List;
import java.util.Map;

import org.example.Documents.DocumentBuilder;
import org.example.Documents.IDocument;

public class MockDataPersistenceService implements IDataPersistenceService {

  @Override
  public IDocument saveDocument(DocumentBuilder scheme, Map<String, List<Double>> conf) {
    scheme.setConfigurations(conf);
    var report = AnalysisReportAPI.createReport(scheme);
    System.out.println("[MOCK] Zapisano dokument: " + report.toString());
    return report;
  }
}
