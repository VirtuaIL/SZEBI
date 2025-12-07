package org.example;

import java.util.List;

interface IAnalysisStrategy {
  List<AlertEventType> analyze(String content);

  public class DefaultStrategy implements IAnalysisStrategy {
    @Override
    public List<AlertEventType> analyze(String content) {
      System.out.println(">>> [ANALIZA MOCK] Analiza: " + content);

      return List.of(AlertEventType.TemperatureExceedsThreshold,
          AlertEventType.PressureExceedsThreshold);
    }
  }
}
