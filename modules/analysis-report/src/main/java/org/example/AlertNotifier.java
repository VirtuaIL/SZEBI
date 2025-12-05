package org.example;

import java.util.ArrayList;
import java.util.List;

public class AlertNotifier {

  // List to store buffered alerts
  private final List<Alert> bufferedAlerts;

  public AlertNotifier() {
    this.bufferedAlerts = new ArrayList<>();
  }

  // Add an alert to the buffer
  public void addAlert(Alert alert) {
    bufferedAlerts.add(alert);
  }

  // Retrieve all buffered alerts
  public Alert[] getBufferedAlerts() {
    return bufferedAlerts.toArray(new Alert[0]);
  }

  // Optionally, clear the buffer after retrieval
  public void clearAlerts() {
    bufferedAlerts.clear();
  }
}
