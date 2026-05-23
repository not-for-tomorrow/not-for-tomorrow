package com.superapp.auth.oauth;

import com.superapp.auth.AuthService;
import com.superapp.auth.Role;
import com.superapp.auth.UserAccount;
import com.superapp.auth.UserAccountRepository;
import com.superapp.auth.dto.TokenPairResponse;
import com.superapp.auth.dto.ClientInfo;
import com.superapp.auth.oauth.dto.OAuthCallbackResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class GoogleOAuthService {

    private final GoogleOAuthProperties props;
    private final OAuthStateStore stateStore;
    private final UserAccountRepository userRepository;
    private final AuthService authService;
    private final RestTemplate restTemplate = new RestTemplate();

    public GoogleOAuthService(
        GoogleOAuthProperties props,
        OAuthStateStore stateStore,
        UserAccountRepository userRepository,
        AuthService authService
    ) {
        this.props = props;
        this.stateStore = stateStore;
        this.userRepository = userRepository;
        this.authService = authService;
    }

    public Map<String, String> buildAuthorizeUrl() {
        ensureConfigured();
        String state = stateStore.issueState();
        String url = "https://accounts.google.com/o/oauth2/v2/auth"
            + "?client_id=" + enc(props.getClientId())
            + "&redirect_uri=" + enc(props.getRedirectUri())
            + "&response_type=code"
            + "&scope=" + enc("openid email profile")
            + "&state=" + enc(state)
            + "&access_type=online"
            + "&prompt=consent";
        Map<String, String> body = new HashMap<String, String>();
        body.put("provider", "GOOGLE");
        body.put("authorizeUrl", url);
        body.put("state", state);
        return body;
    }

    public OAuthCallbackResponse handleCallback(String code, String state, ClientInfo clientInfo) {
        ensureConfigured();
        if (!stateStore.consumeValid(state)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OAuth state");
        }
        String accessToken = exchangeCodeForToken(code);
        GoogleProfile profile = fetchGoogleProfile(accessToken);
        if (profile.email == null || profile.email.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google account email missing");
        }

        String email = profile.email.trim().toLowerCase();
        UserAccount user = userRepository.findByEmail(email).orElse(null);
        boolean created = false;
        if (user == null) {
            user = new UserAccount();
            user.setEmail(email);
            user.setDisplayName(profile.name != null && !profile.name.trim().isEmpty() ? profile.name.trim() : email);
            user.setPasswordHash("{oauth-google}");
            user.setRoles(Collections.singleton(Role.MEMBER));
            user = userRepository.save(user);
            created = true;
        }
        TokenPairResponse tokens = authService.issueTokenPairForUser(user, clientInfo);
        return new OAuthCallbackResponse("GOOGLE", tokens, created);
    }

    private String exchangeCodeForToken(String code) {
        String form = "code=" + enc(code)
            + "&client_id=" + enc(props.getClientId())
            + "&client_secret=" + enc(props.getClientSecret())
            + "&redirect_uri=" + enc(props.getRedirectUri())
            + "&grant_type=authorization_code";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> entity = new HttpEntity<String>(form, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
            "https://oauth2.googleapis.com/token",
            HttpMethod.POST,
            entity,
            Map.class
        );
        Object token = response.getBody() != null ? response.getBody().get("access_token") : null;
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to get Google access token");
        }
        return token.toString();
    }

    private GoogleProfile fetchGoogleProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<Void>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
            "https://www.googleapis.com/oauth2/v2/userinfo",
            HttpMethod.GET,
            entity,
            Map.class
        );
        Map body = response.getBody();
        GoogleProfile p = new GoogleProfile();
        if (body != null) {
            Object email = body.get("email");
            Object name = body.get("name");
            p.email = email != null ? email.toString() : null;
            p.name = name != null ? name.toString() : null;
        }
        return p;
    }

    private void ensureConfigured() {
        if (isBlank(props.getClientId()) || isBlank(props.getClientSecret()) || isBlank(props.getRedirectUri())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google OAuth config is missing");
        }
    }

    private String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    private boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    private static class GoogleProfile {
        private String email;
        private String name;
    }
}
