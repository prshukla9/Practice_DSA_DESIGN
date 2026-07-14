package com.healthcare.common.events;

import java.io.Serializable;
import java.time.Instant;

public class NotificationEvent implements Serializable {
    private String notificationId;
    private String patientId;
    private String channel;
    private String message;
    private String eventType;
    private Instant createdAt;

    public NotificationEvent() {}

    public NotificationEvent(String notificationId, String patientId, String channel,
                             String message, String eventType, Instant createdAt) {
        this.notificationId = notificationId;
        this.patientId = patientId;
        this.channel = channel;
        this.message = message;
        this.eventType = eventType;
        this.createdAt = createdAt;
    }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
