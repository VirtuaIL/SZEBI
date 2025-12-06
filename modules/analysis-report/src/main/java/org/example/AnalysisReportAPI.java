package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.example.Documents.DocumentScheme;
import org.example.Documents.IDocument;
import org.example.Generator.DocumentGenerator;
import org.example.Generator.DocumentGeneratorID;

public class AnalysisReportAPI {
  private List<IAlertNotifier> notifiers = new ArrayList<>();
  private HashMap<IDocumentGeneratorService, List<DocumentGenerator>> generators = new HashMap<>();

  // Generate a report based on a DocumentScheme
  public IDocument generateReport(DocumentScheme documentScheme) {
    return null;
  }

  // Get all available reports
  public List<IDocument> getAvailableReports(DocumentGeneratorID id) {
    return null;
  }

  // Create a new DocumentScheme
  public DocumentScheme newDocumentScheme() {
    return null;
  }

  // Create an AlertNotifier for a given DocumentGeneratorID
  public void subscribeToAlertNotifier(IAlertNotifier notifier) {
    notifiers.add(notifier);
  }

  public void bindDocumentGenerator(IDocumentGeneratorService generatorService) {
    var documentGenerators = generatorService.build(DocumentGenerator.builder());
    generators.put(generatorService, documentGenerators);
  }

  public void unBindDocumentGenerator(IDocumentGeneratorService generatorService) {

  }

}
