package org.example;

public class MockAlertNotifier implements IAlertNotifier {

  @Override
  public void notify(String analysisUUID, AlertEvent alertEvent) {
    System.out.println(analysisUUID + alertEvent.getTimestamp().toString() + alertEvent.getMessage());
  }

}
