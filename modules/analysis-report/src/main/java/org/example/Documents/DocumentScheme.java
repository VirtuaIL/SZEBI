package org.example.Documents;

import java.time.Period;
import java.util.HashMap;

public class DocumentScheme {
  private HashMap<ConfigurationType, Number> configurations;
  private Period period;

  // public void configure(ConfigurationType optionName, Number option) {
  // configurations.put(optionName, option);
  // }

  public IDocument build() {
    throw new UnsupportedOperationException("Unimplemented method 'build'");
  }

  public void temperatureOff() {
    configurations.remove(ConfigurationType.Temperature);
  }

  public void temperatureOn(Number temperature) {
  }

  public void HumidityOn(Number humidity) {
  }

  public void HumidityOff() {
  }

  public void PressureOn(Number Pressure) {
  }

  public void PressureOff() {
  }

  // HashMap<Object, IDocument> getAvailableStrategies() {
  // throw new UnsupportedOperationException("Unimplemented method 'build'");
  // }
}
