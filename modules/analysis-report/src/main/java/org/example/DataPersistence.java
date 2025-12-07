package org.example;

import java.util.LinkedList;
import java.util.Queue;

import org.example.DTO.Raport;
import org.example.interfaces.IAnalyticsData;

public class DataPersistence {
  private final IAnalyticsData analyticsData;
  private final Queue<IDocument> buffer = new LinkedList<>();

  public DataPersistence(IAnalyticsData analyticsData) {
    this.analyticsData = analyticsData;
  }

  public void addDocument(IDocument document) {
    buffer.add(document);
    processBuffer();
  }

  private Raport toRaport(IDocument document) {
    Raport raport = new Raport();

    raport.setCzasWygenerowania(document.getDateFrom());
    raport.setId(document.getId().hashCode());
    raport.setTypRaportu(document.getDocumentType());
    raport.setOpis("-----    OPISU BRAK    ------");
    raport.setZakresDo(document.getCreationDate());
    raport.setZawartosc(document.getContent());

    return raport;
  }

  private void processBuffer() {
    while (!buffer.isEmpty()) {
      IDocument cached = buffer.poll();
      try {
        this.analyticsData.saveReport(toRaport(cached));
      } catch (Exception e) {
        buffer.add(cached);
        break;
      }
    }
  }

  void save(IDocument document) {
    this.analyticsData.saveReport(toRaport(document));
  }
}
