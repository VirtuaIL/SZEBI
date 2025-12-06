package org.example;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.example.Documents.DocumentBuilder;
import org.example.Documents.IDocument;
import org.example.Documents.Report;
import org.example.interfaces.IAnalyticsData;

public class AnalysisReportAPI {
  private final List<IAlertNotifier> notifiers = new ArrayList<>();
  private final Map<IDocumentGeneratorService, List<DocumentGenerator>> serviceGeneratorsMap = new HashMap<>();
  private final DataPersistence dataStorage;
  public static final AquisitionProxyService aquisitionService = new AquisitionProxyService();

  public AnalysisReportAPI(IAnalyticsData datastorage) {
    this.dataStorage = new DataPersistence(datastorage);
  }

  static public Report createReport(DocumentBuilder scheme) {
    return new Report(scheme);
  }

  public void sendDocumentScheme(Function<DocumentBuilder, DocumentBuilder> documentBuilderFunc,
      IDataPersistenceService dataService) {


    var documentbuilder = documentBuilderFunc
        .apply(new DocumentBuilder(AnalysisReportAPI.aquisitionService.getLabelValues()));
    System.out.println(AnalysisReportAPI.aquisitionService.getLabelValues());
    var document = dataService.saveDocument(documentbuilder, AnalysisReportAPI.aquisitionService.getLabelValues());
    this.dataStorage.addDocument(document);
  }

  // public void sendDocumentScheme(
  // DocumentBuilder scheme,
  // IDataPersistenceService dataService
  // ) {
  // var document = dataService.saveDocument(scheme,
  // AnalysisReportAPI.aquisitionService.getLabelValue());
  // this.dataStorage.addDocument(document);
  // }

  public void subscribeToAlertNotifier(IAlertNotifier notifier) {
    notifiers.add(notifier);
  }

  public void bindDocumentGenerator(IDocumentGeneratorService generatorService, IDataPersistenceService dataService) {
    var documentGenerators = generatorService.build(DocumentGenerator.builder(dataService, this.dataStorage));
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
