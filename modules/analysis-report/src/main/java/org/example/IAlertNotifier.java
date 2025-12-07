package org.example;

import java.util.Date;

public interface IAlertNotifier {
  void notify(Analysis analysis, AlertEventType alertEventType);
}
