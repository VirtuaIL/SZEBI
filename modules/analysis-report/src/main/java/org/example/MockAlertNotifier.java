package org.example;

public class MockAlertNotifier implements IAlertNotifier {

  @Override
  public void notify(String analysisUUID, AlertEventType alertEventType) {
    System.out.println(analysisUUID + alertEventType.getDate().toString() + alertEventType.getMessage());
  }

}
