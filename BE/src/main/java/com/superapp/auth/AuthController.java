package com.superapp.auth;

import com.superapp.auth.dto.LoginRequest;
import com.superapp.auth.dto.MfaLoginVerifyRequest;
import com.superapp.auth.dto.MfaSettingsRequest;
import com.superapp.auth.dto.MfaVerifyRequest;
import com.superapp.auth.dto.PasswordResetConfirmRequest;
import com.superapp.auth.dto.PasswordResetRequest;
import com.superapp.auth.dto.RefreshRequest;
import com.superapp.auth.dto.RegisterRequest;
import com.superapp.auth.dto.SessionInfoResponse;
import com.superapp.auth.dto.TokenPairResponse;
import com.superapp.auth.dto.TotpSetupConfirmRequest;
import com.superapp.auth.dto.TotpQrRequest;
import com.superapp.auth.dto.TotpSetupResponse;
import com.superapp.auth.dto.ClientInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final MfaService mfaService;
    private final TotpService totpService;
    private final QrCodeService qrCodeService;
    private final SessionAuditService sessionAuditService;

    public AuthController(
        AuthService authService,
        PasswordResetService passwordResetService,
        MfaService mfaService,
        TotpService totpService,
        QrCodeService qrCodeService,
        SessionAuditService sessionAuditService
    ) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.mfaService = mfaService;
        this.totpService = totpService;
        this.qrCodeService = qrCodeService;
        this.sessionAuditService = sessionAuditService;
    }

    @PostMapping("/register")
    public TokenPairResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        return authService.register(request, buildClientInfo(httpRequest));
    }

    @PostMapping("/login")
    public TokenPairResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, buildClientInfo(httpRequest));
    }

    @PostMapping("/refresh")
    public TokenPairResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.getRefreshToken().trim());
    }

    @PostMapping("/logout")
    public Map<String, String> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logoutCurrent(request.getRefreshToken().trim());
        Map<String, String> body = new HashMap<String, String>();
        body.put("message", "logged out current session");
        return body;
    }

    @DeleteMapping("/logout-all")
    public Map<String, Object> logoutAll(
        @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        int revoked = authService.logoutAllByAccessToken(authorization);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("message", "logged out all sessions");
        body.put("revokedSessions", revoked);
        return body;
    }

    @GetMapping("/sessions")
    public List<SessionInfoResponse> sessions(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestHeader(value = "X-Refresh-Token", required = false) String currentRefreshToken
    ) {
        return authService.listSessionsByAccessToken(authorization, currentRefreshToken);
    }

    @DeleteMapping("/sessions/{id}")
    public Map<String, String> revokeSession(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable("id") Long id
    ) {
        authService.revokeSessionById(authorization, id);
        Map<String, String> body = new HashMap<String, String>();
        body.put("message", "session revoked");
        return body;
    }

    @PostMapping("/password-reset/request")
    public Map<String, Object> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request, HttpServletRequest httpRequest) {
        String token = passwordResetService.requestReset(request.getEmail(), extractIp(httpRequest));
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("message", "If this email exists, a password reset instruction has been created");
        body.put("devResetToken", token);
        return body;
    }

    @PostMapping("/password-reset/confirm")
    public Map<String, String> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirmReset(request.getToken(), request.getNewPassword());
        Map<String, String> body = new HashMap<String, String>();
        body.put("message", "password reset successful");
        return body;
    }

    @PostMapping("/mfa/challenge")
    public Map<String, Object> requestMfaChallenge(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        HttpServletRequest httpRequest
    ) {
        UserAccount user = authService.getCurrentUser(authorization);
        String code = mfaService.issueChallenge(user, buildClientInfo(httpRequest));
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("message", "MFA challenge generated");
        body.put("devCode", code);
        return body;
    }

    @PostMapping("/mfa/verify")
    public Map<String, String> verifyMfaChallenge(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @Valid @RequestBody MfaVerifyRequest request,
        HttpServletRequest httpRequest
    ) {
        UserAccount user = authService.getCurrentUser(authorization);
        mfaService.verifyChallenge(user, request.getCode(), buildClientInfo(httpRequest));
        Map<String, String> body = new HashMap<String, String>();
        body.put("message", "MFA verification successful");
        return body;
    }

    @PostMapping("/login/mfa/verify")
    public TokenPairResponse verifyMfaForLogin(
        @Valid @RequestBody MfaLoginVerifyRequest request,
        HttpServletRequest httpRequest
    ) {
        String email = mfaService.verifyLoginChallenge(request.getChallengeTicket(), request.getCode(), buildClientInfo(httpRequest));
        UserAccount user = authService.getCurrentUserByEmail(email);
        return authService.issueTokenPairForUser(user, buildClientInfo(httpRequest));
    }

    @PostMapping("/mfa/settings")
    public Map<String, Object> updateMfaSettings(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestBody MfaSettingsRequest request
    ) {
        UserAccount user = authService.getCurrentUser(authorization);
        user.setMfaEnabled(request.isEnabled());
        authService.save(user);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("message", "MFA setting updated");
        body.put("enabled", Boolean.valueOf(user.isMfaEnabled()));
        return body;
    }

    @PostMapping("/mfa/totp/setup")
    public TotpSetupResponse startTotpSetup(
        @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        UserAccount user = authService.getCurrentUser(authorization);
        return totpService.startSetup(user);
    }

    @PostMapping("/mfa/totp/confirm")
    public Map<String, Object> confirmTotpSetup(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @Valid @RequestBody TotpSetupConfirmRequest request,
        HttpServletRequest httpRequest
    ) {
        UserAccount user = authService.getCurrentUser(authorization);
        totpService.confirmSetup(user, request.getCode());
        authService.save(user);
        sessionAuditService.log(SessionEventType.TOTP_SETUP_CONFIRMED, user.getEmail(), buildClientInfo(httpRequest), "totp setup confirmed");
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("message", "TOTP setup confirmed");
        body.put("mfaEnabled", Boolean.valueOf(user.isMfaEnabled()));
        body.put("mfaTotpEnabled", Boolean.valueOf(user.isMfaTotpEnabled()));
        return body;
    }

    @PostMapping("/mfa/totp/disable")
    public Map<String, Object> disableTotp(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        HttpServletRequest httpRequest
    ) {
        UserAccount user = authService.getCurrentUser(authorization);
        totpService.disableTotp(user);
        authService.save(user);
        sessionAuditService.log(SessionEventType.TOTP_DISABLED, user.getEmail(), buildClientInfo(httpRequest), "totp disabled");
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("message", "TOTP disabled");
        body.put("mfaTotpEnabled", Boolean.valueOf(false));
        return body;
    }

    @PostMapping("/mfa/totp/recovery/regenerate")
    public Map<String, Object> regenerateRecoveryCodes(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        HttpServletRequest httpRequest
    ) {
        UserAccount user = authService.getCurrentUser(authorization);
        java.util.List<String> codes = totpService.regenerateRecoveryCodes(user);
        authService.save(user);
        sessionAuditService.log(SessionEventType.TOTP_RECOVERY_REGENERATED, user.getEmail(), buildClientInfo(httpRequest), "totp recovery regenerated");
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("message", "Recovery codes regenerated");
        body.put("recoveryCodes", codes);
        return body;
    }

    @PostMapping("/mfa/totp/qrcode")
    public Map<String, String> generateTotpQr(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @Valid @RequestBody TotpQrRequest request
    ) {
        authService.getCurrentUser(authorization);
        Map<String, String> body = new HashMap<String, String>();
        body.put("imageDataUrl", qrCodeService.toPngDataUrl(request.getOtpauthUrl()));
        return body;
    }

    private ClientInfo buildClientInfo(HttpServletRequest request) {
        ClientInfo c = new ClientInfo();
        c.setIpAddress(extractIp(request));
        c.setUserAgent(request.getHeader("User-Agent"));
        c.setDeviceLabel(request.getHeader("X-Device-Label"));
        return c;
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.trim().isEmpty()) {
            String[] parts = forwarded.split(",");
            return parts[0].trim();
        }
        return request.getRemoteAddr();
    }
}
