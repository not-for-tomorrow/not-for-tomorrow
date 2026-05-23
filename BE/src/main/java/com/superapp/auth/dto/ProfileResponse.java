package com.superapp.auth.dto;

import java.util.Set;

public class ProfileResponse {
    private String email;
    private String phone;
    private String displayName;
    private boolean mfaEnabled;
    private boolean mfaTotpEnabled;
    private Set<String> roles;

    public ProfileResponse(
        String email,
        String phone,
        String displayName,
        boolean mfaEnabled,
        boolean mfaTotpEnabled,
        Set<String> roles
    ) {
        this.email = email;
        this.phone = phone;
        this.displayName = displayName;
        this.mfaEnabled = mfaEnabled;
        this.mfaTotpEnabled = mfaTotpEnabled;
        this.roles = roles;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPhone() {
        return phone;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public boolean isMfaTotpEnabled() {
        return mfaTotpEnabled;
    }
}
