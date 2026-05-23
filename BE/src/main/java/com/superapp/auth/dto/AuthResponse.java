package com.superapp.auth.dto;

import java.util.Set;

public class AuthResponse {
    private String token;
    private String tokenType;
    private long expiresIn;
    private String email;
    private String displayName;
    private Set<String> roles;

    public AuthResponse(String token, String tokenType, long expiresIn, String email, String displayName, Set<String> roles) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.email = email;
        this.displayName = displayName;
        this.roles = roles;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<String> getRoles() {
        return roles;
    }
}
