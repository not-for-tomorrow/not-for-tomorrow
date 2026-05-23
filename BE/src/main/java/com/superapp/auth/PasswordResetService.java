package com.superapp.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasswordResetService {

    private final UserAccountRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordPolicyService passwordPolicyService;
    private final PasswordEncoder passwordEncoder;
    private final SessionAuditService sessionAuditService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentMap<String, Instant> resetRequestCooldown = new ConcurrentHashMap<String, Instant>();

    @Value("${app.auth.password-reset-expiration-seconds:1800}")
    private long resetExpirationSeconds;

    @Value("${app.auth.password-reset-expose-token:true}")
    private boolean exposeToken;

    @Value("${app.auth.password-reset-request-cooldown-seconds:60}")
    private long requestCooldownSeconds;

    public PasswordResetService(
        UserAccountRepository userRepository,
        PasswordResetTokenRepository passwordResetTokenRepository,
        PasswordPolicyService passwordPolicyService,
        PasswordEncoder passwordEncoder,
        SessionAuditService sessionAuditService
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordPolicyService = passwordPolicyService;
        this.passwordEncoder = passwordEncoder;
        this.sessionAuditService = sessionAuditService;
    }

    public String requestReset(String rawEmail, String clientIp) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase();
        enforceCooldown(email, clientIp);
        passwordResetTokenRepository.deleteByExpiresAtBefore(Instant.now());
        if (!userRepository.existsByEmail(email)) {
            return null;
        }

        passwordResetTokenRepository.deleteByEmail(email);
        String plainToken = generateToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setEmail(email);
        token.setTokenHash(hash(plainToken));
        token.setExpiresAt(Instant.now().plusSeconds(resetExpirationSeconds));
        passwordResetTokenRepository.save(token);
        sessionAuditService.log(SessionEventType.PASSWORD_RESET_REQUESTED, email, toClientInfo(clientIp), "password reset requested");

        return exposeToken ? plainToken : null;
    }

    public void confirmReset(String plainToken, String newPassword) {
        if (plainToken == null || plainToken.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token is required");
        }
        passwordPolicyService.validate(newPassword);

        String tokenHash = hash(plainToken.trim());
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reset token"));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token expired or already used");
        }

        UserAccount user = userRepository.findByEmail(resetToken.getEmail())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token is no longer valid"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        sessionAuditService.log(
            SessionEventType.PASSWORD_RESET_CONFIRMED,
            user.getEmail(),
            new com.superapp.auth.dto.ClientInfo(),
            "password reset confirmed"
        );
    }

    private void enforceCooldown(String email, String clientIp) {
        String key = (email == null ? "" : email) + "|" + (clientIp == null ? "" : clientIp.trim());
        Instant now = Instant.now();
        Instant until = resetRequestCooldown.get(key);
        if (until != null && until.isAfter(now)) {
            long seconds = Math.max(1, until.getEpochSecond() - now.getEpochSecond());
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Please wait " + seconds + "s before requesting another reset");
        }
        resetRequestCooldown.put(key, now.plusSeconds(requestCooldownSeconds));
    }

    private com.superapp.auth.dto.ClientInfo toClientInfo(String clientIp) {
        com.superapp.auth.dto.ClientInfo c = new com.superapp.auth.dto.ClientInfo();
        c.setIpAddress(clientIp);
        c.setDeviceLabel("password-reset");
        return c;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
