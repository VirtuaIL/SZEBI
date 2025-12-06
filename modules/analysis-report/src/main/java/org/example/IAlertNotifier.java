package org.example;

import java.util.Date;

import org.example.Documents.Analysis;

public interface IAlertNotifier {
  boolean notify(Analysis analysis, Date alertDate, AlertEventType alertEventType);
}
