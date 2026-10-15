package com.booksys.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordUtil {
    public static final int ITERATIONS = 600_000;
    private static final SecureRandom RANDOM = new SecureRandom();
    private PasswordUtil() {}

    public static String hash(String password) {
        if (password == null || password.isEmpty() || password.length() > 128)
            throw new IllegalArgumentException("密码长度无效");
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return "pbkdf2$" + ITERATIONS + "$" + encode(salt) + "$" + encode(derive(password, salt, ITERATIONS));
    }

    public static boolean verify(String password, String stored) {
        if (password == null || password.length() > 128 || stored == null) return false;
        if (isLegacy(stored)) {
            return MessageDigest.isEqual(MD5Util.encrypt(password).getBytes(StandardCharsets.US_ASCII),
                    stored.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
        }
        try {
            String[] parts = stored.split("\\$", -1);
            if (parts.length != 4 || !"pbkdf2".equals(parts[0])) return false;
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 100_000 || iterations > 1_200_000) return false;
            byte[] salt = Base64.getUrlDecoder().decode(parts[2]);
            byte[] expected = Base64.getUrlDecoder().decode(parts[3]);
            if (salt.length != 16 || expected.length != 32) return false;
            return MessageDigest.isEqual(expected, derive(password, salt, iterations));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean needsUpgrade(String stored) {
        if (isLegacy(stored)) return true;
        try { return Integer.parseInt(stored.split("\\$", -1)[1]) < ITERATIONS; }
        catch (RuntimeException e) { return false; }
    }

    private static boolean isLegacy(String stored) { return stored != null && stored.matches("(?i)[0-9a-f]{32}"); }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec key = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(key).getEncoded(); }
        catch (GeneralSecurityException e) { throw new IllegalStateException("Password hashing unavailable", e); }
        finally { key.clearPassword(); }
    }

    private static String encode(byte[] bytes) { return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
}
