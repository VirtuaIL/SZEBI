package org.example.Documents;

import java.time.Period;

public final class Report extends Document implements IDocument<Report> {
  public Report(DocumentScheme scheme) {

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
