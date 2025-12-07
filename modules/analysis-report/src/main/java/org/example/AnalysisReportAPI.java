package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.example.Documents.IDocument;
import org.example.Documents.Report;
import org.example.interfaces.IAnalyticsData;

public class AnalysisReportAPI {
  private final List<IAlertNotifier> notifiers = new ArrayList<>();
  private final Map<IDocumentGeneratorService, List<DocumentGenerator>> serviceGeneratorsMap = new HashMap<>();
  private final DataPersistence dataStorage;
  private static final IDocumentFactoryService defaultDocumentFactory = new DefaultDocumentFactoryService();

  public static final AquisitionProxy getAquisitionProxy() {
    return AquisitionProxy.singleton;
  }

  public AnalysisReportAPI(IAnalyticsData datastorage) {
    this.dataStorage = new DataPersistence(datastorage);
  }

  public void sendDocumentScheme(
      Function<IDocument.Builder, IDocument.Builder> documentBuilderFunc,
      IDocumentFactoryService dataService) {

    IDocument.Builder documentBuilder = documentBuilderFunc.apply(IDocument.buildReport());
    var document = dataService.enqueueDocument(documentBuilder.build());

    this.dataStorage.addDocument(document);
  }

  public void sendDocumentScheme(Function<IDocument.Builder, IDocument.Builder> documentBuilderFunc) {
    IDocument.Builder documentbuilder = documentBuilderFunc.apply(new IDocument.Builder<>(Report::new));

    var document = defaultDocumentFactory.enqueueDocument(documentbuilder.build());
    this.dataStorage.addDocument(document);
  }

  public void subscribeToAlertNotifier(IAlertNotifier notifier) {
    notifiers.add(notifier);
  }

  public void bindDocumentGenerator(
      IDocumentGeneratorService generatorService,
      IDocumentFactoryService dataService) {

    var documentGenerators = generatorService
        .build(DocumentGenerator.builder(dataService, this.dataStorage));
    serviceGeneratorsMap.put(generatorService, documentGenerators);
  }

  public void bindDocumentGenerator(IDocumentGeneratorService generatorService) {

    var documentGenerators = generatorService
        .build(DocumentGenerator.builder(defaultDocumentFactory, this.dataStorage));

    serviceGeneratorsMap.put(generatorService, documentGenerators);
  }

  public List<DocumentGenerator> getBindedDocumentGenerators(IDocumentGeneratorService generatorService) {
    return serviceGeneratorsMap.get(generatorService);
  }

  public boolean unBindDocumentGenerator(
      IDocumentGeneratorService generatorService,
      DocumentGenerator generator) {

    var documentGenerators = serviceGeneratorsMap.get(generatorService);
    generator.shutdown();

    return documentGenerators.remove(generator);
  }

  public boolean unbindAllDocumentGenerators(IDocumentGeneratorService generatorService) {
    var documentGenerators = serviceGeneratorsMap.get(generatorService);
    documentGenerators.forEach(DocumentGenerator::shutdown);

    if (serviceGeneratorsMap.remove(generatorService) != null) {
      return true;
    }

    return false;
  }

}
