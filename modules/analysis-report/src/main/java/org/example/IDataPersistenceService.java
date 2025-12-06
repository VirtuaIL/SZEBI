package org.example;

import org.example.Documents.DocumentScheme;
import org.example.Documents.IDocument;

public interface IDataPersistenceService {
  public IDocument saveDocument(DocumentScheme scheme);
}
