package org.example;

public enum DeviceType {
  Temperature("temperatura_C"),
  Humidity("humidity"),
  Pressure("pressure"),
  Power("power"),
  Luminosity("luminosity"),
  CO2Level("co2_level"),
  NoiseLevel("noise_level");

  private final String value;

  DeviceType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static DeviceType fromString(String type) {
    if (type == null) {
      return null;
    }
    String normalized = type.trim().replace("\"", "").replace("'", "");
    // Checks for exact matches
    for (DeviceType ct : DeviceType.values()) {
      if (ct.value.equalsIgnoreCase(normalized) || ct.name().equalsIgnoreCase(normalized)) {
        return ct;
      }
    }
    // Fallback heuristic for common types
    String lower = normalized.toLowerCase();
    if (lower.contains("temp")) {
      System.out.println("[DeviceType] Partial match 'temp' -> Temperature");
      return Temperature;
    }
    if (lower.contains("jasn") || lower.contains("swiat") || lower.contains("lum")) {
      System.out.println("[DeviceType] Partial match light/lum -> Luminosity");
      return Luminosity;
    }

    System.out.println("[DeviceType] Failed to match: '" + type + "' (norm: '" + normalized + "')");
    return null;
  }

  @Override
  public String toString() {
    return value;
  }
}
