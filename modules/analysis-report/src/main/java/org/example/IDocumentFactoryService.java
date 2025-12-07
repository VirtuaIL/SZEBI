package org.example;

import org.example.Documents.IDocument;

interface IDocumentFactoryService {
  public IDocument enqueueDocument(IDocument scheme);
}
