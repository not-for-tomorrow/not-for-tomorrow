package com.superapp.auth;

import com.superapp.auth.dto.ClientInfo;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MfaService {

    private static class MfaChallenge {
        private String code;
        private Instant expiresAt;
        private int failedAttempts;
    }

    public static class LoginMfaChallenge {
        private final String ticket;
        private final String devCode;

        public LoginMfaChallenge(String ticket, String devCode) {
            this.ticket = ticket;
            this.devCode = devCode;
        }

        public String getTicket() {
            return ticket;
        }

        public String getDevCode() {
            return devCode;
        }
    }

    private final ConcurrentMap<String, MfaChallenge> challenges = new ConcurrentHashMap<String, MfaChallenge>();
    private final ConcurrentMap<String, Instant> challengeCooldown = new ConcurrentHashMap<String, Instant>();
    private final MfaLoginChallengeStore loginChallengeStore;
    private final SessionAuditService sessionAuditService;
    private final TotpService totpService;
    private final UserAccountRepository userAccountRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.auth.mfa.expiration-seconds:300}")
    private long mfaExpirationSeconds;

    @Value("${app.auth.mfa.cooldown-seconds:30}")
    private long mfaCooldownSeconds;

    @Value("${app.auth.mfa.max-failed-attempts:5}")
    private int mfaMaxFailedAttempts;

    @Value("${app.auth.mfa.expose-code:true}")
    private boolean exposeCode;

    public MfaService(
        SessionAuditService sessionAuditService,
        TotpService totpService,
        UserAccountRepository userAccountRepository,
        MfaLoginChallengeStore loginChallengeStore
    ) {
        this.sessionAuditService = sessionAuditService;
        this.totpService = totpService;
        this.userAccountRepository = userAccountRepository;
        this.loginChallengeStore = loginChallengeStore;
    }

    public String issueChallenge(UserAccount user, ClientInfo clientInfo) {
        String email = user.getEmail();
        enforceCooldown(email);

        MfaChallenge challenge = new MfaChallenge();
        challenge.code = generateSixDigits();
        challenge.expiresAt = Instant.now().plusSeconds(mfaExpirationSeconds);
        challenge.failedAttempts = 0;
        challenges.put(email, challenge);

        sessionAuditService.log(SessionEventType.MFA_CHALLENGE_REQUESTED, email, clientInfo, "mfa challenge created");
        return exposeCode ? challenge.code : null;
    }

    public void verifyChallenge(UserAccount user, String code, ClientInfo clientInfo) {
        String email = user.getEmail();
        MfaChallenge challenge = challenges.get(email);
        if (challenge == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active MFA challenge");
        }
        if (challenge.expiresAt.isBefore(Instant.now())) {
            challenges.remove(email);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA challenge expired");
        }
        if (code == null || code.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA code is required");
        }

        if (!challenge.code.equals(code.trim())) {
            challenge.failedAttempts++;
            if (challenge.failedAttempts >= Math.max(1, mfaMaxFailedAttempts)) {
                challenges.remove(email);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA challenge locked due to too many failed attempts");
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid MFA code");
        }

        challenges.remove(email);
        sessionAuditService.log(SessionEventType.MFA_VERIFIED, email, clientInfo, "mfa verified");
    }

    public LoginMfaChallenge issueLoginChallenge(UserAccount user, ClientInfo clientInfo) {
        String email = user.getEmail();
        enforceCooldown("login:" + email);
        String ticket = generateTicket();
        MfaLoginChallengeRecord challenge = new MfaLoginChallengeRecord();
        challenge.setEmail(email);
        challenge.setCode(generateSixDigits());
        challenge.setExpiresAt(Instant.now().plusSeconds(mfaExpirationSeconds));
        challenge.setFailedAttempts(0);
        loginChallengeStore.put(ticket, challenge, mfaExpirationSeconds);
        sessionAuditService.log(SessionEventType.MFA_CHALLENGE_REQUESTED, email, clientInfo, "mfa login challenge created");
        return new LoginMfaChallenge(ticket, exposeCode ? challenge.getCode() : null);
    }

    public String verifyLoginChallenge(String ticket, String code, ClientInfo clientInfo) {
        if (ticket == null || ticket.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA challenge ticket is required");
        }
        MfaLoginChallengeRecord challenge = loginChallengeStore.get(ticket.trim());
        if (challenge == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid MFA challenge ticket");
        }
        if (challenge.getExpiresAt().isBefore(Instant.now())) {
            loginChallengeStore.remove(ticket.trim());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA challenge expired");
        }
        if (code == null || code.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA code is required");
        }
        if (!challenge.getCode().equals(code.trim())) {
            UserAccount user = userAccountRepository.findByEmail(challenge.getEmail()).orElse(null);
            if (user != null && user.isMfaTotpEnabled() && totpService.verifyTotpOrRecovery(user, code.trim())) {
                userAccountRepository.save(user);
                loginChallengeStore.remove(ticket.trim());
                sessionAuditService.log(SessionEventType.MFA_VERIFIED, challenge.getEmail(), clientInfo, "mfa login verified via totp/recovery");
                return challenge.getEmail();
            }
            challenge.setFailedAttempts(challenge.getFailedAttempts() + 1);
            if (challenge.getFailedAttempts() >= Math.max(1, mfaMaxFailedAttempts)) {
                loginChallengeStore.remove(ticket.trim());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA challenge locked due to too many failed attempts");
            }
            long ttlSeconds = Math.max(1L, challenge.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond());
            loginChallengeStore.put(ticket.trim(), challenge, ttlSeconds);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid MFA code");
        }
        loginChallengeStore.remove(ticket.trim());
        sessionAuditService.log(SessionEventType.MFA_VERIFIED, challenge.getEmail(), clientInfo, "mfa login challenge verified");
        return challenge.getEmail();
    }

    private void enforceCooldown(String key) {
        Instant now = Instant.now();
        Instant until = challengeCooldown.get(key);
        if (until != null && until.isAfter(now)) {
            long wait = Math.max(1, until.getEpochSecond() - now.getEpochSecond());
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Please wait " + wait + "s before requesting another MFA code");
        }
        challengeCooldown.put(key, now.plusSeconds(mfaCooldownSeconds));
    }

    private String generateSixDigits() {
        int value = secureRandom.nextInt(1000000);
        return String.format("%06d", Integer.valueOf(value));
    }

    private String generateTicket() {
        byte[] buf = new byte[24];
        secureRandom.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
