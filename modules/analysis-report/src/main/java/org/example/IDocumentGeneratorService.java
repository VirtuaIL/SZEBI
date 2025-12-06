package org.example;

import java.util.List;

import org.example.Documents.IDocument;
import org.example.DocumentGenerator;

public interface IDocumentGeneratorService {
  List<DocumentGenerator> build(DocumentGenerator.Builder builder);
}
