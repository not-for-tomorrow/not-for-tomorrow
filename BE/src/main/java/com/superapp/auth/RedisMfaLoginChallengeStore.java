package com.superapp.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnProperty(name = "app.auth.mfa.challenge-store", havingValue = "redis")
public class RedisMfaLoginChallengeStore implements MfaLoginChallengeStore {

    private static final String PREFIX = "mfa:login:challenge:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisMfaLoginChallengeStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void put(String ticket, MfaLoginChallengeRecord challenge, long ttlSeconds) {
        try {
            String key = PREFIX + ticket;
            String value = objectMapper.writeValueAsString(challenge);
            long safeTtl = Math.max(1L, ttlSeconds);
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(safeTtl));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to store MFA challenge in Redis", ex);
        }
    }

    @Override
    public MfaLoginChallengeRecord get(String ticket) {
        try {
            String value = redisTemplate.opsForValue().get(PREFIX + ticket);
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            return objectMapper.readValue(value, MfaLoginChallengeRecord.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load MFA challenge from Redis", ex);
        }
    }

    @Override
    public void remove(String ticket) {
        redisTemplate.delete(PREFIX + ticket);
    }
}
