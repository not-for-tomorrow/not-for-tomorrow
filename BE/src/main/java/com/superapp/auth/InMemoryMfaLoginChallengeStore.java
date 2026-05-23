package com.superapp.auth;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.auth.mfa.challenge-store", havingValue = "memory", matchIfMissing = true)
public class InMemoryMfaLoginChallengeStore implements MfaLoginChallengeStore {

    private final ConcurrentMap<String, MfaLoginChallengeRecord> store = new ConcurrentHashMap<String, MfaLoginChallengeRecord>();

    @Override
    public void put(String ticket, MfaLoginChallengeRecord challenge, long ttlSeconds) {
        store.put(ticket, challenge);
    }

    @Override
    public MfaLoginChallengeRecord get(String ticket) {
        return store.get(ticket);
    }

    @Override
    public void remove(String ticket) {
        store.remove(ticket);
    }
}
