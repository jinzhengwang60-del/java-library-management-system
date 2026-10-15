package com.booksys.filter;

import com.booksys.dao.impl.UserDaoImpl;
import com.booksys.entity.User;
import com.booksys.util.AppConfig;
import com.booksys.util.RememberToken;
import javax.servlet.http.*;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;

public final class RememberMeHelper {
    private static final String COOKIE_NAME = "rememberMe";
    private static final RememberToken TOKENS = new RememberToken(secret(), Clock.systemUTC());
    private RememberMeHelper() {}

    private static byte[] secret() {
        String configured = AppConfig.get("remember.key", "BOOKSYS_REMEMBER_KEY", "");
        if (!configured.isBlank()) return Base64.getDecoder().decode(configured);
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    public static void setRememberMe(HttpServletRequest request, HttpServletResponse response, User user) {
        clearRememberMe(request, response);
        setCookie(request, response, TOKENS.issue(user.getId(), user.getPassword()), RememberToken.MAX_AGE, cookiePath(request));
    }

    public static void clearRememberMe(HttpServletRequest request, HttpServletResponse response) {
        setCookie(request, response, "", 0, cookiePath(request));
        if (!"/".equals(cookiePath(request))) setCookie(request, response, "", 0, "/");
    }

    private static String cookiePath(HttpServletRequest request) {
        return request.getContextPath().isEmpty() ? "/" : request.getContextPath();
    }

    private static void setCookie(HttpServletRequest request, HttpServletResponse response,
                                  String value, int age, String path) {
        String expires = java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(
                (age == 0 ? java.time.Instant.EPOCH : java.time.Instant.now().plusSeconds(age))
                        .atZone(java.time.ZoneOffset.UTC));
        response.addHeader("Set-Cookie", COOKIE_NAME + "=" + value + "; Path=" + path
                + "; Max-Age=" + age + "; Expires=" + expires
                + "; HttpOnly; SameSite=Lax" + (request.isSecure() ? "; Secure" : ""));
    }

    public static User getRememberedUser(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (!COOKIE_NAME.equals(cookie.getName())) continue;
            Integer id = RememberToken.userId(cookie.getValue());
            if (id == null) continue;
            User user = new UserDaoImpl().findById(id);
            if (user != null && Integer.valueOf(1).equals(user.getStatus())
                    && TOKENS.verify(cookie.getValue(), user.getPassword())) return user;
        }
        return null;
    }
}
