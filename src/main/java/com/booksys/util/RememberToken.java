package com.booksys.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;

/** A signed, expiring user-id token. The password hash is never put in the cookie. */
public final class RememberToken {
    public static final int MAX_AGE = 7 * 24 * 60 * 60;
    private final byte[] key;
    private final Clock clock;

    public RememberToken(byte[] key, Clock clock) {
        if (key == null || key.length < 32) throw new IllegalArgumentException("Remember key must contain at least 32 random bytes");
        this.key = key.clone();
        this.clock = clock;
    }

    public String issue(int userId, String passwordHash) {
        String payload = userId + "." + (clock.instant().getEpochSecond() + MAX_AGE);
        return payload + "." + signature(payload, passwordHash);
    }

    public static Integer userId(String token) {
        if (token == null || !token.matches("[0-9]{1,10}\\.[0-9]{1,15}\\.[A-Za-z0-9_-]{43}")) return null;
        try { int id = Integer.parseInt(token.split("\\.")[0]); return id > 0 ? id : null; }
        catch (NumberFormatException e) { return null; }
    }

    public boolean verify(String token, String passwordHash) {
        if (userId(token) == null || passwordHash == null) return false;
        String[] parts = token.split("\\.");
        try {
            long expires = Long.parseLong(parts[1]);
            long now = clock.instant().getEpochSecond();
            if (expires <= now || expires > now + MAX_AGE) return false;
            String payload = parts[0] + "." + parts[1];
            return MessageDigest.isEqual(signature(payload, passwordHash).getBytes(StandardCharsets.US_ASCII),
                    parts[2].getBytes(StandardCharsets.US_ASCII));
        } catch (IllegalArgumentException e) { return false; }
    }

    private String signature(String payload, String passwordHash) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] bytes = mac.doFinal((payload + "." + passwordHash).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (GeneralSecurityException e) { throw new IllegalStateException("Remember token signing unavailable", e); }
    }
}
