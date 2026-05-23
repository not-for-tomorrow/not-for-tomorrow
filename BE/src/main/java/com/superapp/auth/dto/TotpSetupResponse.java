package com.superapp.auth.dto;

import java.util.List;

public class TotpSetupResponse {
    private String secret;
    private String otpauthUrl;
    private List<String> recoveryCodes;
    private long expiresInSeconds;

    public TotpSetupResponse(String secret, String otpauthUrl, List<String> recoveryCodes, long expiresInSeconds) {
        this.secret = secret;
        this.otpauthUrl = otpauthUrl;
        this.recoveryCodes = recoveryCodes;
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getSecret() {
        return secret;
    }

    public String getOtpauthUrl() {
        return otpauthUrl;
    }

    public List<String> getRecoveryCodes() {
        return recoveryCodes;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }
}
