package com.superapp.auth.oauth;

import com.superapp.auth.oauth.dto.OAuthCallbackResponse;
import com.superapp.auth.dto.ClientInfo;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth/oauth2")
public class OAuthSkeletonController {

    private final GoogleOAuthService googleOAuthService;

    public OAuthSkeletonController(GoogleOAuthService googleOAuthService) {
        this.googleOAuthService = googleOAuthService;
    }

    @GetMapping("/providers")
    public Map<String, Object> providers() {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("providers", Arrays.stream(OAuthProvider.values()).map(Enum::name).collect(Collectors.toList()));
        body.put("status", "skeleton");
        return body;
    }

    @GetMapping("/{provider}/authorize-url")
    public Map<String, String> authorizeUrl(@PathVariable("provider") String provider) {
        OAuthProvider parsed;
        try {
            parsed = OAuthProvider.valueOf(provider.toUpperCase());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown provider");
        }

        if (parsed == OAuthProvider.GOOGLE) {
            try {
                Map<String, String> real = googleOAuthService.buildAuthorizeUrl();
                Map<String, String> body = new HashMap<String, String>();
                body.put("provider", real.get("provider"));
                body.put("message", "Google OAuth is enabled (callback endpoint ready).");
                body.put("authorizeUrl", real.get("authorizeUrl"));
                body.put("state", real.get("state"));
                return body;
            } catch (ResponseStatusException ex) {
                Map<String, String> body = new HashMap<String, String>();
                body.put("provider", "GOOGLE");
                body.put("message", "Google OAuth config missing. Set GOOGLE_CLIENT_ID/SECRET/REDIRECT_URI.");
                body.put("status", "not_configured");
                body.put("authorizeUrl", "/pending/oauth2/google");
                return body;
            }
        }

        Map<String, String> body = new HashMap<String, String>();
        body.put("provider", parsed.name());
        body.put("message", "OAuth2 skeleton only. Real provider integration is pending.");
        body.put("authorizeUrl", "/pending/oauth2/" + parsed.name().toLowerCase());
        return body;
    }

    @GetMapping("/google/callback")
    public OAuthCallbackResponse googleCallback(
        @RequestParam("code") String code,
        @RequestParam("state") String state,
        HttpServletRequest request
    ) {
        if (code == null || code.trim().isEmpty() || state == null || state.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing code/state");
        }
        return googleOAuthService.handleCallback(code.trim(), state.trim(), buildClientInfo(request));
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
