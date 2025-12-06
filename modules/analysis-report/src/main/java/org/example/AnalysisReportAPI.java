package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.example.Documents.DocumentScheme;
import org.example.Generator.DocumentGenerator;

public class AnalysisReportAPI {
  private List<IAlertNotifier> notifiers = new ArrayList<>();
  private Map<IDocumentGeneratorService, List<DocumentGenerator>> serviceGeneratorsMap = new HashMap<>();

  public void sendDocumentScheme(DocumentScheme scheme, IDataPersistenceService dataService) {
    dataService.saveDocument(scheme);
  }

  public void subscribeToAlertNotifier(IAlertNotifier notifier) {
    notifiers.add(notifier);
  }

  public void bindDocumentGenerator(IDocumentGeneratorService generatorService, IDataPersistenceService dataService) {
    var documentGenerators = generatorService.build(DocumentGenerator.builder(dataService));
    serviceGeneratorsMap.put(generatorService, documentGenerators);
  }

  public List<DocumentGenerator> getBindedDocumentGenerators(IDocumentGeneratorService generatorService) {
    return serviceGeneratorsMap.get(generatorService);
  }

  public boolean unBindDocumentGenerator(IDocumentGeneratorService generatorService, DocumentGenerator generator) {
    var documentGenerators = serviceGeneratorsMap.get(generatorService);

    for (var x : documentGenerators) {
      x.shutdown();
    }

    return documentGenerators.remove(generator);
  }

}
