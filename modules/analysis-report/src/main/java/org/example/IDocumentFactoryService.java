package org.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.example.Documents.IDocument;

public interface IDocumentFactoryService {
  public IDocument createDocument(IDocument.Builder scheme, Map<String, List<Double>> conf);
}
