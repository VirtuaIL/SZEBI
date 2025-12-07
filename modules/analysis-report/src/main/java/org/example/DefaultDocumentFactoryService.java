package org.example;

import org.example.Documents.IDocument;

public class DefaultDocumentFactoryService implements IDocumentFactoryService {

  @Override
  public IDocument enqueueDocument(IDocument document) {
    System.out.println("[MOCK] Zapisano dokument: " + document.toString());
    return document;
  }
}
