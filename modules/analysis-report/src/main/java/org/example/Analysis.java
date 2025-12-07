package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

final class Analysis extends IDocument.Document {
  private IAnalysisStrategy strategy = new IAnalysisStrategy.DefaultStrategy();
  private List<AlertEventType> alerts = new ArrayList<>();

  public Analysis(IDocument.Scheme scheme) {
    super(scheme);
    strategy.analyze(this);
  }

  void notifyNotifiers(List<IAlertNotifier> notifiers) {
    if (alerts.isEmpty())
      return;

    for (var notifier : notifiers) {
      for (var alert : alerts) {
        notifier.notify(this.getId(), alert);
      }
    }

    this.alerts.clear();
  }

  @Override
  protected String generateContent(Map<String, Double> data) {
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

  @Override
  public String getDocumentType() {
    return "Analiza";
  }

}
