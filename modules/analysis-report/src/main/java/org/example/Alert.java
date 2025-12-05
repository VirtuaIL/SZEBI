package org.example;

import java.util.Date;
import org.example.Documents.Analysis;

public class Alert {
  private final Date alertDate;
  private final Analysis analysis;
  private final AlertEvent alertEvent;
  private final String contextData;

  public Alert(Date alertDate, Analysis analysis, AlertEvent alertEvent, String contextData) {
    this.alertDate = alertDate;
    this.analysis = analysis;
    this.alertEvent = alertEvent;
    this.contextData = contextData;
  }

  /**
   * Returns the date when the alert was generated
   * 
   * @return Date of the alert
   */
  public Date getAlertDate() {
    return alertDate;
  }

  /**
   * Returns the Analysis associated with this alert
   * 
   * @return Analysis object
   */
  public Analysis getAnalysis() {
    return analysis;
  }

  /**
   * Returns the AlertEvent that triggered this alert
   * 
   * @return AlertEvent object
   */
  public AlertEvent getAlertEvent() {
    return alertEvent;
  }

  /**
   * Returns the context data associated with this alert
   * 
   * @return ContextData object
   */
  public String getContextData() {
    return contextData;
  }

  @Override
  public String toString() {
    return "Alert{" +
        "alertDate=" + alertDate +
        ", analysis=" + analysis +
        ", alertEvent=" + alertEvent +
        ", contextData=" + contextData +
        '}';
  }
}
