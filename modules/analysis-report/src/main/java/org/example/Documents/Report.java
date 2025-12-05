package org.example.Documents;

public class Report implements IDocument {
  @Override
  public String generateJson() {
    return "Report saved";
  }

}
