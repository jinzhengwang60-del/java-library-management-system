package com.booksys.filter;

import com.booksys.entity.User;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * 免密登录辅助类（Remember Me）
 * 通过 Cookie 实现
 */
public class RememberMeHelper {

    private static final String COOKIE_NAME = "rememberMe";
    private static final int MAX_AGE = 7 * 24 * 60 * 60; // 7天

    /** 写入记住我 cookie，存储 用户名|MD5密码 */
    public static void setRememberMe(HttpServletResponse response, String username, String md5Password) {
        try {
            String value = URLEncoder.encode(username + "|" + md5Password, "UTF-8");
            Cookie c = new Cookie(COOKIE_NAME, value);
            c.setMaxAge(MAX_AGE);
            c.setPath("/");
            c.setHttpOnly(true);
            response.addCookie(c);
        } catch (Exception ignored) {}
    }

    /** 清除记住我 cookie */
    public static void clearRememberMe(HttpServletResponse response) {
        Cookie c = new Cookie(COOKIE_NAME, "");
        c.setMaxAge(0);
        c.setPath("/");
        response.addCookie(c);
    }

    /** 从 cookie 读取并自动登录 */
    public static User getRememberedUser(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (COOKIE_NAME.equals(c.getName()) && c.getValue() != null && !c.getValue().isEmpty()) {
                try {
                    String value = URLDecoder.decode(c.getValue(), "UTF-8");
                    String[] arr = value.split("\\|");
                    if (arr.length == 2) {
                        com.booksys.dao.IUserDao dao = new com.booksys.dao.impl.UserDaoImpl();
                        return dao.findByUsernameAndPassword(arr[0], arr[1]);
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
    }
}
