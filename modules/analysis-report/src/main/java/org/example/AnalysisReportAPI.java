package org.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import org.example.interfaces.IAnalyticsData;

public class AnalysisReportAPI {
  private static final List<IAlertNotifier> notifiers = new CopyOnWriteArrayList<>();
  private final Map<IDocumentGeneratorService, List<DocumentGenerator>> serviceGeneratorsMap = new HashMap<>();
  private final DataPersistence dataStorage;
  private static final IDocumentFactoryService defaultDocumentFactory = new DefaultDocumentFactoryService();
  private static final AquisitionProxy aquisitionProxy = new AquisitionProxy();

  static List<IAlertNotifier> getAlertNotifiers() {
    return Collections.unmodifiableList(notifiers);
  }

  public static final AquisitionProxy getAquisitionProxy() {
    return aquisitionProxy;
  }

  public AnalysisReportAPI(IAnalyticsData datastorage) {
    this.dataStorage = new DataPersistence(datastorage);
  }

  public void sendDocumentScheme(
      Function<IDocument.Scheme, IDocument.Scheme> documentBuilderFunc,
      IDocumentFactoryService dataService) {

    IDocument.Scheme documentBuilder = documentBuilderFunc.apply(IDocument.reportBuilder());
    var document = dataService.enqueueDocument(documentBuilder.build());

    this.dataStorage.addDocument(document);
  }

  public void sendDocumentScheme(IDocument.Scheme documentBuilder) {
    var document = defaultDocumentFactory.enqueueDocument(documentBuilder.build());
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

  public static IDocument.Scheme<Report> newReportScheme() {
    return IDocument.reportBuilder();
  }

  public static IDocument.Scheme<Analysis> newAnalysisAnalysis() {
    return IDocument.analysisBuilder();
  }

  public static Set<String> getAvailableMetrics() {
    return aquisitionProxy.getLabelsSet();
  }

}
