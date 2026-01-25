package org.example;

import java.time.LocalDateTime;
import java.util.Objects;

public class AlertEvent {
    private final AlertEventType type;
    private final LocalDateTime timestamp;
    private final String deviceId;

    public AlertEvent(AlertEventType type) {
        this.type = type;
        this.timestamp = LocalDateTime.now();
        this.deviceId = null;
    }

    public AlertEvent(AlertEventType type, LocalDateTime timestamp) {
        this.type = type;
        this.timestamp = timestamp;
        this.deviceId = null;
    }

    public AlertEvent(AlertEventType type, String deviceId) {
        this.type = type;
        this.timestamp = LocalDateTime.now();
        this.deviceId = deviceId;
    }

    public AlertEvent(AlertEventType type, LocalDateTime timestamp, String deviceId) {
        this.type = type;
        this.timestamp = timestamp;
        this.deviceId = deviceId;
    }

    public AlertEventType getType() {
        return type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getMessage() {
        return type.getMessage();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlertEvent that = (AlertEvent) o;
        return type == that.type && Objects.equals(deviceId, that.deviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, deviceId);
    }

    @Override
    public String toString() {
        return "AlertEvent{" +
                "type=" + type +
                ", timestamp=" + timestamp +
                ", deviceId='" + deviceId + '\'' +
                '}';
    }
}
