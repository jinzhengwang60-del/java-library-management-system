package com.booksys.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@WebFilter(urlPatterns = "/*")
public class CsrfFilter implements Filter {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        request.setCharacterEncoding("UTF-8");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "same-origin");
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean staticResource = path.startsWith("/css/") || path.startsWith("/images/") || path.startsWith("/upload/");
        if (!staticResource) response.setHeader("Cache-Control", "no-store");
        boolean safe = "GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod()) || "OPTIONS".equals(request.getMethod());
        HttpSession session = request.getSession(!staticResource && safe);
        if (session != null && session.getAttribute("csrfToken") == null) {
            synchronized (session) {
                if (session.getAttribute("csrfToken") == null) {
                    byte[] bytes = new byte[32];
                    RANDOM.nextBytes(bytes);
                    session.setAttribute("csrfToken", Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
                }
            }
        }
        if (!safe) {
            // Form tokens require parsing multipart bodies. Parse explicitly because
            // some containers return null parameters after an upload limit failure.
            if (request.getContentType() != null && request.getContentType().toLowerCase(java.util.Locale.ROOT)
                    .startsWith("multipart/form-data")) {
                try { request.getParts(); }
                catch (IllegalStateException e) {
                    response.sendError(413, "上传内容超过大小限制"); return;
                } catch (ServletException e) {
                    response.sendError(400, "上传内容无效"); return;
                }
            }
            String expected = session == null ? null : (String) session.getAttribute("csrfToken");
            String provided = request.getHeader("X-CSRF-Token");
            try {
                if (provided == null) provided = request.getParameter("_csrf");
            } catch (IllegalStateException e) {
                response.sendError(413, "上传内容超过大小限制"); return;
            }
            if (expected == null || provided == null || provided.length() > 128 ||
                    !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8))) {
                response.sendError(403, "请求无效，请刷新页面后重试"); return;
            }
        }
        chain.doFilter(req, resp);
    }
}
