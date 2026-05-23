package com.superapp.auth.dto;

import java.time.Instant;

public class SessionAuditEventResponse {
    private Long id;
    private String eventType;
    private String userEmail;
    private String ipAddress;
    private String deviceLabel;
    private String detail;
    private Instant createdAt;

    public SessionAuditEventResponse(
        Long id,
        String eventType,
        String userEmail,
        String ipAddress,
        String deviceLabel,
        String detail,
        Instant createdAt
    ) {
        this.id = id;
        this.eventType = eventType;
        this.userEmail = userEmail;
        this.ipAddress = ipAddress;
        this.deviceLabel = deviceLabel;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getDeviceLabel() {
        return deviceLabel;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

