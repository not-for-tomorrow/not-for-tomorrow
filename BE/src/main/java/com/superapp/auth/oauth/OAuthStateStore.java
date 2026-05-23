package com.superapp.auth.oauth;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class OAuthStateStore {
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Long> states = new ConcurrentHashMap<String, Long>();
    private static final long TTL_MILLIS = 5 * 60 * 1000;

    public String issueState() {
        byte[] buf = new byte[24];
        random.nextBytes(buf);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        states.put(state, System.currentTimeMillis() + TTL_MILLIS);
        return state;
    }

    public boolean consumeValid(String state) {
        Long exp = states.remove(state);
        return exp != null && exp >= System.currentTimeMillis();
    }
}

