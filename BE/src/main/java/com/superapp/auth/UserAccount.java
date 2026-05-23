package com.superapp.auth;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user_accounts")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(nullable = false, length = 120)
    private String displayName;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean mfaEnabled = false;

    @Column(nullable = false)
    private boolean mfaTotpEnabled = false;

    @Column(length = 64)
    private String mfaTotpSecret;

    @Column(length = 1200)
    private String mfaRecoveryCodeHashes;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<Role>();

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public boolean isMfaTotpEnabled() {
        return mfaTotpEnabled;
    }

    public void setMfaTotpEnabled(boolean mfaTotpEnabled) {
        this.mfaTotpEnabled = mfaTotpEnabled;
    }

    public String getMfaTotpSecret() {
        return mfaTotpSecret;
    }

    public void setMfaTotpSecret(String mfaTotpSecret) {
        this.mfaTotpSecret = mfaTotpSecret;
    }

    public String getMfaRecoveryCodeHashes() {
        return mfaRecoveryCodeHashes;
    }

    public void setMfaRecoveryCodeHashes(String mfaRecoveryCodeHashes) {
        this.mfaRecoveryCodeHashes = mfaRecoveryCodeHashes;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}
