package org.example;

import java.time.LocalDateTime;
import java.util.Objects;

public class AlertEvent {
    private final AlertEventType type;
    private final LocalDateTime timestamp;

    public AlertEvent(AlertEventType type) {
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    public AlertEvent(AlertEventType type, LocalDateTime timestamp) {
        this.type = type;
        this.timestamp = timestamp;
    }

    public AlertEventType getType() {
        return type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return type.getMessage();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlertEvent that = (AlertEvent) o;
        return type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public String toString() {
        return "AlertEvent{" +
                "type=" + type +
                ", timestamp=" + timestamp +
                '}';
    }
}
