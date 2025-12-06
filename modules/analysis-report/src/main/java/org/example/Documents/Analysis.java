package org.example.Documents;

public final class Analysis extends IDocument.Document {
  public Analysis(IDocument.Builder scheme) {
    super(scheme);
  }

  public String getDocumentType() {
    return "Analiza";
  }

  @Override
  protected String generateContent(String data) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'generateContent'");
  }

}
