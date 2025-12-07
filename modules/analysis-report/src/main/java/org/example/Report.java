package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

final class Report extends IDocument.Document {
  public Report(IDocument.Builder builder) {
    super(builder);
  }

  @Override
  public String getDocumentType() {
    return "Raport";
  }

  /**
   * Funckja używana w constructorze
   *
   * @param data String
   * @return [TODO:return]
   */
  @Override
  protected String generateContent(String data) {
    ObjectMapper mapper = new ObjectMapper();

    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    String content = "";
    try {
      content = mapper.writeValueAsString(data);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      content = "{\"error\": \"Failed to generate JSON\"}";
    }

    return content;
  }
}
