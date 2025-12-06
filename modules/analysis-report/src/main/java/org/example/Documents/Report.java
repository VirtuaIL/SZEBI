package org.example.Documents;

public final class Report extends Document implements IDocument<Report> {

  public Report(DocumentBuilder scheme) {

    // setContent(this.content);
  }

  public String generateJson() {
    return "Report saved";
  }

  public String getDocumentType() {
    return "Raport";
  }

  @Override
  public Report getThis() {
    return this;
  }

}
