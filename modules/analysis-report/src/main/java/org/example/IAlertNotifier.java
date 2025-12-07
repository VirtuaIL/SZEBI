package org.example;

public interface IAlertNotifier {
  void notify(String analysisUUID, AlertEventType alertEventType);
}
