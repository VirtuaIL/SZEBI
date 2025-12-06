package org.example.Documents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class Report extends Document implements IDocument<Report> {

  public Report(DocumentBuilder scheme) {
    ObjectMapper mapper = new ObjectMapper();

    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    String content = "";
    try {
      content = mapper.writeValueAsString(scheme);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      content = "{\"error\": \"Failed to generate JSON\"}";
    }
    setContent(content);
  }

  public String generateJson() {
    return getContent() != null ? getContent().toString() : "";
  }

  @Override
  public String getDocumentType() {
    return "Raport";
  }

  @Override
  public Report getThis() {
    return this;
  }
}
