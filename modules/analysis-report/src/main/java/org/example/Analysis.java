package org.example;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

final class Analysis extends IDocument.Document {
  private List<AlertEventType> alerts = new ArrayList<>();

  public Analysis(IDocument.Builder scheme) {
    super(scheme);
  }

  void sendAlert(List<IAlertNotifier> notifiers) {
    if (alerts.isEmpty()) {
      return;
    }

    for (var notifier : notifiers) {
      for (var alert : alerts) {
        notifier.notify(this, alert);
      }
    }
  }

  public String getDocumentType() {
    return "Analiza";
  }

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
