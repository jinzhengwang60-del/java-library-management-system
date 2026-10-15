package com.booksys;

import com.booksys.util.RememberToken;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class RememberTokenTest {
    private final byte[] key = new byte[32];
    private final Clock now = Clock.fixed(Instant.parse("2026-10-15T00:00:00Z"), ZoneOffset.UTC);
    @Test void signedCookieContainsOnlyIdExpiryAndSignature() {
        Arrays.fill(key, (byte) 42);
        RememberToken tokens = new RememberToken(key, now);
        String token = tokens.issue(7, "private-password-hash");
        assertEquals(7, RememberToken.userId(token));
        assertTrue(tokens.verify(token, "private-password-hash"));
        assertFalse(token.contains("private-password-hash"));
        assertFalse(tokens.verify(token, "changed-password-hash"));
        assertFalse(tokens.verify("8" + token.substring(1), "private-password-hash"));
        byte[] otherKey = key.clone(); otherKey[0]++;
        assertFalse(new RememberToken(otherKey, now).verify(token, "private-password-hash"));
    }
    @Test void expiryIsEnforcedAtTheBoundary() {
        String token = new RememberToken(key, now).issue(1, "hash");
        assertTrue(new RememberToken(key, Clock.offset(now, Duration.ofSeconds(RememberToken.MAX_AGE - 1)))
                .verify(token, "hash"));
        assertFalse(new RememberToken(key, Clock.offset(now, Duration.ofSeconds(RememberToken.MAX_AGE)))
                .verify(token, "hash"));
        assertFalse(new RememberToken(key, Clock.offset(now, Duration.ofSeconds(-1))).verify(token, "hash"));
    }
    @Test void invalidIdsAndCookieFormatsFailClosed() {
        for (String value : new String[]{"", "user=1", "1.2.x", "-1.2.x", "2147483648.123456789012345." + "x".repeat(43)})
            assertNull(RememberToken.userId(value));
        assertNull(RememberToken.userId(null));
        assertThrows(IllegalArgumentException.class, () -> new RememberToken(new byte[31], now));
    }
}
