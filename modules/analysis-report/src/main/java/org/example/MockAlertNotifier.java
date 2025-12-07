package org.example;

import java.util.Date;

public class MockAlertNotifier implements IAlertNotifier {

  @Override
  public void notify(Analysis analysis, AlertEventType alertEventType) {
    System.out.println(analysis.toString() + alertEventType.getDate().toString() + alertEventType.getMessage());
  }

}
