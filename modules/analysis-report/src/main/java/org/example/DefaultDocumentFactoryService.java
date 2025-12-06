package org.example;

import java.util.List;
import java.util.Map;

import org.example.Documents.IDocument;

public class DefaultDocumentFactoryService implements IDocumentFactoryService {

  @Override
  public IDocument createDocument(IDocument.Builder scheme, Map<String, List<Double>> conf) {
    scheme.setConfigurations(conf);
    var report = scheme.buildReport();
    System.out.println("[MOCK] Zapisano dokument: " + report.toString());
    return report;
  }
}
