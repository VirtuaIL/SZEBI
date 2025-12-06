package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.example.Documents.IDocument;
import org.example.interfaces.IAnalyticsData;

public class AnalysisReportAPI {
  private final List<IAlertNotifier> notifiers = new ArrayList<>();
  private final Map<IDocumentGeneratorService, List<DocumentGenerator>> serviceGeneratorsMap = new HashMap<>();
  private final DataPersistence dataStorage;
  private static final DefaultDocumentFactoryService defaultDocumentFactory = new DefaultDocumentFactoryService();
  public static final AquisitionProxyService aquisitionService = new AquisitionProxyService();

  public AnalysisReportAPI(IAnalyticsData datastorage) {
    this.dataStorage = new DataPersistence(datastorage);
  }

  public void sendDocumentScheme(Function<IDocument.Builder, IDocument.Builder> documentBuilderFunc,
      IDocumentFactoryService dataService) {

    var documentbuilder = documentBuilderFunc
        .apply(new IDocument.Builder(AnalysisReportAPI.aquisitionService.getLabelValues()));

    System.out.println(AnalysisReportAPI.aquisitionService.getLabelValues());
    var document = dataService.createDocument(documentbuilder, AnalysisReportAPI.aquisitionService.getLabelValues());
    this.dataStorage.addDocument(document);
  }

  public void sendDocumentScheme(Function<IDocument.Builder, IDocument.Builder> documentBuilderFunc) {

    var documentbuilder = documentBuilderFunc
        .apply(new IDocument.Builder(AnalysisReportAPI.aquisitionService.getLabelValues()));

    System.out.println(AnalysisReportAPI.aquisitionService.getLabelValues());
    var document = AnalysisReportAPI.defaultDocumentFactory.createDocument(documentbuilder,
        AnalysisReportAPI.aquisitionService.getLabelValues());
    this.dataStorage.addDocument(document);
  }

  public void subscribeToAlertNotifier(IAlertNotifier notifier) {
    notifiers.add(notifier);
  }

  public void bindDocumentGenerator(IDocumentGeneratorService generatorService,
      IDocumentFactoryService dataService) {
    var documentGenerators = generatorService.build(DocumentGenerator.builder(dataService, this.dataStorage));
    serviceGeneratorsMap.put(generatorService, documentGenerators);
  }

  public void bindDocumentGenerator(IDocumentGeneratorService generatorService) {
    var documentGenerators = generatorService
        .build(DocumentGenerator.builder(AnalysisReportAPI.defaultDocumentFactory, this.dataStorage));
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
