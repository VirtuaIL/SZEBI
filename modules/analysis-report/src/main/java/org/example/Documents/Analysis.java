package org.example.Documents;

public final class Analysis extends Document implements IDocument<Analysis> {
  public Analysis(DocumentBuilder scheme) {

  }

  public String generateJson() {
    throw new UnsupportedOperationException("Unimplemented method 'generateJson'");
  }

  public String getDocumentType() {
    return "Analiza";
  }

  @Override
  public Analysis getThis() {
    return this;
  }

}
