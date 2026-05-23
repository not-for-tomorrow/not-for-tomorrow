package com.superapp.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    @Value("${app.auth.jwt-secret}")
    private String jwtSecret;

    @Value("${app.auth.jwt-expiration-seconds}")
    private long jwtExpirationSeconds;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] raw = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(raw);
    }

    public String issueToken(String email) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(jwtExpirationSeconds);
        return Jwts.builder()
            .setSubject(email)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(exp))
            .signWith(secretKey)
            .compact();
    }

    public String extractEmail(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
            return claims.getSubject();
        } catch (Exception ex) {
            return null;
        }
    }

    public long getJwtExpirationSeconds() {
        return jwtExpirationSeconds;
    }
}
