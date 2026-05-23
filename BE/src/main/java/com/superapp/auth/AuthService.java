package com.superapp.auth;

import com.superapp.auth.dto.AuthResponse;
import com.superapp.auth.dto.LoginRequest;
import com.superapp.auth.dto.RegisterRequest;
import com.superapp.auth.dto.ClientInfo;
import com.superapp.auth.dto.SessionInfoResponse;
import com.superapp.auth.dto.TokenPairResponse;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final PasswordPolicyService passwordPolicyService;
    private final SessionTokenRepository sessionTokenRepository;
    private final SessionMetricsService sessionMetricsService;
    private final SessionAuditService sessionAuditService;
    private final MfaService mfaService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.auth.refresh-expiration-seconds}")
    private long refreshExpirationSeconds;

    public AuthService(
        UserAccountRepository userRepository,
        PasswordEncoder passwordEncoder,
        TokenService tokenService,
        PasswordPolicyService passwordPolicyService,
        SessionTokenRepository sessionTokenRepository,
        SessionMetricsService sessionMetricsService,
        SessionAuditService sessionAuditService,
        MfaService mfaService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.passwordPolicyService = passwordPolicyService;
        this.sessionTokenRepository = sessionTokenRepository;
        this.sessionMetricsService = sessionMetricsService;
        this.sessionAuditService = sessionAuditService;
        this.mfaService = mfaService;
    }

    public TokenPairResponse register(RegisterRequest request, ClientInfo clientInfo) {
        String email = request.getEmail().trim().toLowerCase();
        String phone = normalizePhone(request.getPhone());
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already exists");
        }
        passwordPolicyService.validate(request.getPassword());
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setPhone(phone);
        user.setDisplayName(request.getDisplayName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Collections.singleton(Role.MEMBER));
        userRepository.save(user);
        return issueTokenPair(user, clientInfo);
    }

    public TokenPairResponse login(LoginRequest request, ClientInfo clientInfo) {
        String identifier = readIdentifier(request);
        UserAccount user = findByIdentifier(identifier)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        if (user.isMfaEnabled()) {
            MfaService.LoginMfaChallenge challenge = mfaService.issueLoginChallenge(user, clientInfo);
            return TokenPairResponse.mfaRequired(challenge.getTicket(), challenge.getDevCode());
        }
        return issueTokenPair(user, clientInfo);
    }

    public UserAccount getCurrentUser(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }
        String token = bearerToken.substring(7).trim();
        String email = tokenService.extractEmail(token);
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public UserAccount getCurrentUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public AuthResponse issueForUser(UserAccount user) {
        return toAuthResponse(user);
    }

    public TokenPairResponse issueTokenPairForUser(UserAccount user, ClientInfo clientInfo) {
        return issueTokenPair(user, clientInfo);
    }

    public UserAccount save(UserAccount user) {
        return userRepository.save(user);
    }

    public TokenPairResponse refresh(String refreshToken) {
        SessionToken session = sessionTokenRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (session.isRevoked() || session.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired/revoked");
        }
        AuthResponse access = toAuthResponse(session.getUser());
        return new TokenPairResponse(access, session.getRefreshToken(), refreshExpirationSeconds);
    }

    public void logoutCurrent(String refreshToken) {
        SessionToken session = sessionTokenRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        session.setRevoked(true);
        sessionTokenRepository.save(session);
        sessionMetricsService.incRevoked(1);
        ClientInfo c = new ClientInfo();
        c.setIpAddress(session.getIpAddress());
        c.setDeviceLabel(session.getDeviceLabel());
        sessionAuditService.log(
            SessionEventType.SESSION_REVOKED,
            session.getUser().getEmail(),
            c,
            "logout current session"
        );
    }

    public int logoutAllByAccessToken(String bearerToken) {
        UserAccount user = getCurrentUser(bearerToken);
        int count = 0;
        for (SessionToken session : sessionTokenRepository.findByUserAndRevokedFalse(user)) {
            session.setRevoked(true);
            count++;
        }
        sessionTokenRepository.flush();
        sessionMetricsService.incRevoked(count);
        ClientInfo c = new ClientInfo();
        sessionAuditService.log(
            SessionEventType.SESSION_REVOKED,
            user.getEmail(),
            c,
            "logout all sessions count=" + count
        );
        return count;
    }

    public List<SessionInfoResponse> listSessionsByAccessToken(String bearerToken, String currentRefreshToken) {
        UserAccount user = getCurrentUser(bearerToken);
        List<SessionToken> sessions = sessionTokenRepository.findByUserOrderByCreatedAtDesc(user);
        return sessions.stream()
            .map(s -> new SessionInfoResponse(
                s.getId(),
                s.getCreatedAt(),
                s.getExpiresAt(),
                s.isRevoked(),
                currentRefreshToken != null && currentRefreshToken.equals(s.getRefreshToken())
                ,
                s.getIpAddress(),
                s.getUserAgent(),
                s.getDeviceLabel()
            ))
            .collect(Collectors.toList());
    }

    public void revokeSessionById(String bearerToken, Long sessionId) {
        UserAccount user = getCurrentUser(bearerToken);
        SessionToken session = sessionTokenRepository.findByIdAndUser(sessionId, user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        session.setRevoked(true);
        sessionTokenRepository.save(session);
    }

    private TokenPairResponse issueTokenPair(UserAccount user, ClientInfo clientInfo) {
        AuthResponse access = toAuthResponse(user);
        SessionToken session = new SessionToken();
        session.setUser(user);
        session.setRefreshToken(generateRefreshToken());
        session.setExpiresAt(Instant.now().plusSeconds(refreshExpirationSeconds));
        if (clientInfo != null) {
            session.setIpAddress(trimTo(clientInfo.getIpAddress(), 64));
            session.setUserAgent(trimTo(clientInfo.getUserAgent(), 512));
            session.setDeviceLabel(trimTo(clientInfo.getDeviceLabel(), 120));
        }
        sessionTokenRepository.save(session);
        sessionMetricsService.incCreated();
        sessionAuditService.log(
            SessionEventType.SESSION_CREATED,
            user.getEmail(),
            clientInfo,
            "session issued"
        );
        return new TokenPairResponse(access, session.getRefreshToken(), refreshExpirationSeconds);
    }

    private AuthResponse toAuthResponse(UserAccount user) {
        String token = tokenService.issueToken(user.getEmail());
        Set<String> roles = user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
        return new AuthResponse(
            token,
            "Bearer",
            tokenService.getJwtExpirationSeconds(),
            user.getEmail(),
            user.getDisplayName(),
            roles
        );
    }

    private String generateRefreshToken() {
        byte[] buf = new byte[32];
        secureRandom.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private String trimTo(String value, int maxLen) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;
        return v.length() <= maxLen ? v : v.substring(0, maxLen);
    }

    private String readIdentifier(LoginRequest request) {
        String raw = request.getIdentifier();
        if (raw == null || raw.trim().isEmpty()) {
            raw = request.getEmail();
        }
        if (raw == null || raw.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email or phone is required");
        }
        return raw.trim();
    }

    private java.util.Optional<UserAccount> findByIdentifier(String identifier) {
        if (identifier.contains("@")) {
            return userRepository.findByEmail(identifier.toLowerCase());
        }
        String phone = normalizePhone(identifier);
        if (phone == null) {
            return java.util.Optional.empty();
        }
        return userRepository.findByPhone(phone);
    }

    private String normalizePhone(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.isEmpty()) return null;
        if (!value.matches("^[+]?[0-9]{9,15}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid phone format");
        }
        if (value.startsWith("+")) {
            return "+" + value.substring(1).replaceAll("[^0-9]", "");
        }
        return value.replaceAll("[^0-9]", "");
    }
}
