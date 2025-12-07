package org.example;

import java.util.List;

interface IAnalysisStrategy {
  List<AlertEventType> analyze(Analysis content);

  class DefaultStrategy implements IAnalysisStrategy {
    @Override
    public List<AlertEventType> analyze(Analysis analysis) {
      System.out.println(">>> [ANALIZA MOCK] Analiza: " + analysis.getContent());

      return List.of(AlertEventType.TemperatureExceedsThreshold,
          AlertEventType.PressureExceedsThreshold);
    }
  }
}
