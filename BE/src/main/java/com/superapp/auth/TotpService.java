package com.superapp.auth;

import com.superapp.auth.dto.TotpSetupResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TotpService {

    private static class PendingTotpSetup {
        private String secret;
        private List<String> recoveryCodes;
        private Instant expiresAt;
    }

    private final ConcurrentMap<String, PendingTotpSetup> pendingSetups = new ConcurrentHashMap<String, PendingTotpSetup>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretCryptoService secretCryptoService;

    @Value("${app.auth.mfa.totp.setup-expiration-seconds:600}")
    private long setupExpirationSeconds;

    @Value("${app.auth.mfa.totp.issuer:SuperAppPortfolio}")
    private String issuer;

    public TotpService(SecretCryptoService secretCryptoService) {
        this.secretCryptoService = secretCryptoService;
    }

    public TotpSetupResponse startSetup(UserAccount user) {
        String secret = generateBase32Secret();
        List<String> recoveryCodes = generateRecoveryCodes();
        PendingTotpSetup setup = new PendingTotpSetup();
        setup.secret = secret;
        setup.recoveryCodes = recoveryCodes;
        setup.expiresAt = Instant.now().plusSeconds(setupExpirationSeconds);
        pendingSetups.put(user.getEmail(), setup);

        String accountName = urlEncode(user.getEmail());
        String issuerEncoded = urlEncode(issuer);
        String url = "otpauth://totp/" + issuerEncoded + ":" + accountName
            + "?secret=" + secret + "&issuer=" + issuerEncoded + "&algorithm=SHA1&digits=6&period=30";
        return new TotpSetupResponse(secret, url, recoveryCodes, setupExpirationSeconds);
    }

    public void confirmSetup(UserAccount user, String code) {
        PendingTotpSetup setup = pendingSetups.get(user.getEmail());
        if (setup == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No pending TOTP setup");
        }
        if (setup.expiresAt.isBefore(Instant.now())) {
            pendingSetups.remove(user.getEmail());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TOTP setup expired");
        }
        if (!verifyTotp(setup.secret, code, 1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid TOTP code");
        }

        user.setMfaTotpSecret(secretCryptoService.encrypt(setup.secret));
        user.setMfaTotpEnabled(true);
        user.setMfaEnabled(true);
        user.setMfaRecoveryCodeHashes(hashRecoveryCodes(setup.recoveryCodes));
        pendingSetups.remove(user.getEmail());
    }

    public void disableTotp(UserAccount user) {
        user.setMfaTotpEnabled(false);
        user.setMfaTotpSecret(null);
        user.setMfaRecoveryCodeHashes(null);
    }

    public boolean verifyTotpOrRecovery(UserAccount user, String code) {
        String encryptedSecret = user.getMfaTotpSecret();
        if (encryptedSecret != null) {
            String rawSecret = secretCryptoService.decrypt(encryptedSecret);
            if (verifyTotp(rawSecret, code, 1)) {
                return true;
            }
        }
        return consumeRecoveryCode(user, code);
    }

    public List<String> regenerateRecoveryCodes(UserAccount user) {
        if (!user.isMfaTotpEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TOTP is not enabled");
        }
        List<String> recoveryCodes = generateRecoveryCodes();
        user.setMfaRecoveryCodeHashes(hashRecoveryCodes(recoveryCodes));
        return recoveryCodes;
    }

    private boolean consumeRecoveryCode(UserAccount user, String code) {
        if (code == null || code.trim().isEmpty()) return false;
        String stored = user.getMfaRecoveryCodeHashes();
        if (stored == null || stored.trim().isEmpty()) return false;
        String[] hashes = stored.split(",");
        String target = sha256(code.trim());
        List<String> next = new ArrayList<String>();
        boolean matched = false;
        for (String h : hashes) {
            String v = h.trim();
            if (v.isEmpty()) continue;
            if (!matched && v.equals(target)) {
                matched = true;
                continue;
            }
            next.add(v);
        }
        if (matched) {
            user.setMfaRecoveryCodeHashes(String.join(",", next));
        }
        return matched;
    }

    private String hashRecoveryCodes(List<String> recoveryCodes) {
        List<String> hashes = new ArrayList<String>();
        for (String c : recoveryCodes) {
            hashes.add(sha256(c));
        }
        return String.join(",", hashes);
    }

    private List<String> generateRecoveryCodes() {
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < 8; i++) {
            byte[] b = new byte[5];
            secureRandom.nextBytes(b);
            String s = Base64.getUrlEncoder().withoutPadding().encodeToString(b).toUpperCase();
            out.add(s.substring(0, Math.min(8, s.length())));
        }
        return out;
    }

    private boolean verifyTotp(String secretBase32, String code, int window) {
        if (code == null || !code.trim().matches("^[0-9]{6}$")) return false;
        long nowStep = Instant.now().getEpochSecond() / 30L;
        byte[] key = base32Decode(secretBase32);
        for (int i = -window; i <= window; i++) {
            String candidate = totpCodeAtStep(key, nowStep + i);
            if (code.trim().equals(candidate)) return true;
        }
        return false;
    }

    private String totpCodeAtStep(byte[] key, long timeStep) {
        try {
            byte[] counter = new byte[8];
            for (int i = 7; i >= 0; i--) {
                counter[i] = (byte) (timeStep & 0xFF);
                timeStep >>>= 8;
            }
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(counter);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);
            int otp = binary % 1000000;
            return String.format("%06d", Integer.valueOf(otp));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to compute TOTP", ex);
        }
    }

    private String generateBase32Secret() {
        byte[] random = new byte[20];
        secureRandom.nextBytes(random);
        return base32Encode(random);
    }

    private String base32Encode(byte[] bytes) {
        final char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
        StringBuilder out = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                out.append(alphabet[(buffer >> (bitsLeft - 5)) & 31]);
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            out.append(alphabet[(buffer << (5 - bitsLeft)) & 31]);
        }
        return out.toString();
    }

    private byte[] base32Decode(String text) {
        final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        String input = text == null ? "" : text.trim().replace("=", "").toUpperCase();
        int buffer = 0;
        int bitsLeft = 0;
        byte[] out = new byte[(input.length() * 5) / 8];
        int index = 0;
        for (int i = 0; i < input.length(); i++) {
            int val = alphabet.indexOf(input.charAt(i));
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        byte[] trimmed = new byte[index];
        System.arraycopy(out, 0, trimmed, 0, index);
        return trimmed;
    }

    private String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
