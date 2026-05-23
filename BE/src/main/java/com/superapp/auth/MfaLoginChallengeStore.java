package com.superapp.auth;

public interface MfaLoginChallengeStore {
    void put(String ticket, MfaLoginChallengeRecord challenge, long ttlSeconds);
    MfaLoginChallengeRecord get(String ticket);
    void remove(String ticket);
}
