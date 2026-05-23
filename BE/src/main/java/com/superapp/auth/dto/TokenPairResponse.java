package com.superapp.auth.dto;

public class TokenPairResponse {
    private AuthResponse access;
    private String refreshToken;
    private long refreshExpiresIn;
    private boolean mfaRequired;
    private String mfaChallengeTicket;
    private String devMfaCode;

    public TokenPairResponse(AuthResponse access, String refreshToken, long refreshExpiresIn) {
        this(access, refreshToken, refreshExpiresIn, false, null, null);
    }

    public TokenPairResponse(
        AuthResponse access,
        String refreshToken,
        long refreshExpiresIn,
        boolean mfaRequired,
        String mfaChallengeTicket,
        String devMfaCode
    ) {
        this.access = access;
        this.refreshToken = refreshToken;
        this.refreshExpiresIn = refreshExpiresIn;
        this.mfaRequired = mfaRequired;
        this.mfaChallengeTicket = mfaChallengeTicket;
        this.devMfaCode = devMfaCode;
    }

    public static TokenPairResponse mfaRequired(String ticket, String devCode) {
        return new TokenPairResponse(null, null, 0L, true, ticket, devCode);
    }

    public AuthResponse getAccess() {
        return access;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public long getRefreshExpiresIn() {
        return refreshExpiresIn;
    }

    public boolean isMfaRequired() {
        return mfaRequired;
    }

    public String getMfaChallengeTicket() {
        return mfaChallengeTicket;
    }

    public String getDevMfaCode() {
        return devMfaCode;
    }
}
