package com.superapp.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SecretCryptoService {

    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKeySpec keySpec;

    @Value("${app.auth.crypto-key-base64:}")
    private String cryptoKeyBase64;

    @Value("${app.auth.jwt-secret:superapp-portfolio-dev-secret-must-change}")
    private String fallbackSecret;

    @PostConstruct
    public void init() {
        byte[] keyBytes;
        if (cryptoKeyBase64 != null && !cryptoKeyBase64.trim().isEmpty()) {
            keyBytes = Base64.getDecoder().decode(cryptoKeyBase64.trim());
        } else {
            keyBytes = sha256(fallbackSecret);
        }
        if (keyBytes.length < 16) {
            throw new IllegalStateException("Crypto key is too short; require at least 128-bit");
        }
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            byte[] normalized = new byte[32];
            System.arraycopy(keyBytes, 0, normalized, 0, Math.min(keyBytes.length, 32));
            keyBytes = normalized;
        }
        keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            byte[] iv = new byte[12];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(encrypted, 0, out, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt secret", ex);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null) return null;
        try {
            byte[] payload = Base64.getDecoder().decode(cipherText);
            if (payload.length < 13) {
                throw new IllegalStateException("Invalid encrypted payload");
            }
            byte[] iv = new byte[12];
            byte[] encrypted = new byte[payload.length - 12];
            System.arraycopy(payload, 0, iv, 0, 12);
            System.arraycopy(payload, 12, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(128, iv));
            byte[] plain = cipher.doFinal(encrypted);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt secret", ex);
        }
    }

    private byte[] sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to derive crypto key", ex);
        }
    }
}
