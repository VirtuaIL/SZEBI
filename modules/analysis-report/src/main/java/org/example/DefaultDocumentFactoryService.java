package org.example;

public class DefaultDocumentFactoryService implements IDocumentFactoryService {

  @Override
  public IDocument enqueueDocument(IDocument document) {
    System.out.println("[MOCK] Zapisano dokument: " + document.toString());

    if (document instanceof Analysis analysis) {
      analysis.sendAlert(AnalysisReportAPI.getAlertNotifiers());
    }

    return document;
  }
}
