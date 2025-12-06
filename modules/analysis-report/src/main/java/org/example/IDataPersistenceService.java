package org.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.example.Documents.DocumentBuilder;
import org.example.Documents.IDocument;

public interface IDataPersistenceService {
  public IDocument saveDocument(DocumentBuilder scheme, Map<String, List<Double>> conf);
}
