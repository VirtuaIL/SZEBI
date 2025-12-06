package org.example.Documents;

import java.time.Period;

public final class Analysis extends Document implements IDocument<Analysis> {

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
