package org.example;

public interface IAlertNotifier {
  void notify(Analysis analysis, AlertEventType alertEventType);
}
