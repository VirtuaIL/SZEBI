package org.example;

import java.util.HashMap;
import java.util.List;

import org.example.Documents.ConfigurationType;
import org.example.DocumentGenerator;
import org.example.DocumentGenerator.Builder;

public class MockDocumentGeneratorService implements IDocumentGeneratorService {

  private HashMap<ConfigurationType, Number> configurations;

  @Override
  public List<DocumentGenerator> build(Builder builder) {
    return List.of(builder.build());
  }
}
