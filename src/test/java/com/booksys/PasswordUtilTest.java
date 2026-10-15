package com.booksys;

import com.booksys.util.MD5Util;
import com.booksys.util.PasswordUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {
    @Test void newHashesUseDifferentSaltsAndVerifyOnlyTheCorrectPassword() {
        String first = PasswordUtil.hash("123456");
        String second = PasswordUtil.hash("123456");
        assertNotEquals(first, second);
        assertTrue(first.startsWith("pbkdf2$600000$"));
        assertTrue(PasswordUtil.verify("123456", first));
        assertFalse(PasswordUtil.verify("123457", first));
        assertFalse(PasswordUtil.needsUpgrade(first));
    }
    @Test void legacyHashesRemainReadableAndAreMarkedForUpgrade() {
        String legacy = MD5Util.encrypt("123456");
        assertTrue(PasswordUtil.verify("123456", legacy));
        assertTrue(PasswordUtil.verify("123456", legacy.toUpperCase(java.util.Locale.ROOT)));
        assertFalse(PasswordUtil.verify("wrong", legacy));
        assertTrue(PasswordUtil.needsUpgrade(legacy));
    }
    @Test void malformedAndUnreasonablyExpensiveHashesAreRejected() {
        for (String stored : new String[]{"", "garbage", "pbkdf2$999999999$x$y",
                "pbkdf2$600000$not-base64!$value", "pbkdf2$0$$", "pbkdf2$600000$YQ$YQ"}) {
            assertFalse(PasswordUtil.verify("123456", stored), stored);
        }
        assertFalse(PasswordUtil.verify(null, null));
        assertFalse(PasswordUtil.verify("x".repeat(129), MD5Util.encrypt("123456")));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash("x".repeat(129)));
    }
}
