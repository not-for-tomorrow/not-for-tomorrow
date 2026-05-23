package com.superapp.auth.dto;

import java.time.Instant;

public class SessionInfoResponse {
    private Long id;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean revoked;
    private boolean current;
    private String ipAddress;
    private String userAgent;
    private String deviceLabel;

    public SessionInfoResponse(
        Long id,
        Instant createdAt,
        Instant expiresAt,
        boolean revoked,
        boolean current,
        String ipAddress,
        String userAgent,
        String deviceLabel
    ) {
        this.id = id;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
        this.current = current;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.deviceLabel = deviceLabel;
    }

    public Long getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public boolean isCurrent() {
        return current;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getDeviceLabel() {
        return deviceLabel;
    }
}
