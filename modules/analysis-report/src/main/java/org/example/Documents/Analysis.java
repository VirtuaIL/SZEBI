package org.example.Documents;

public class Analysis implements IDocument {
  public void generateReport() {
    System.out.println("Generating Analysis Report...");
  }

  @Override
  public String generateJson() {
    throw new UnsupportedOperationException("Unimplemented method 'generateJson'");
  }
}
