package org.example;

import java.util.List;

import org.example.Generator.DocumentGenerator;

public interface IDocumentGeneratorService {
  List<DocumentGenerator> build(DocumentGenerator.Builder builder);
}
