package org.example;

public interface IAlertNotifier {
  void notify(String analysisUUID, AlertEvent alertEvent);
}
