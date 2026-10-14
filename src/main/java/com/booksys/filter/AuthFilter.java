package com.booksys.filter;

import com.booksys.entity.User;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * 身份验证过滤器
 * 任务书要求：强制身份验证
 * 拦截所有需要登录的请求；未登录跳转到登录页
 */
@WebFilter(urlPatterns = "/*")
public class AuthFilter implements Filter {

    /** 不需要登录的资源后缀 */
    private static final Set<String> PUBLIC_SUFFIXES = new HashSet<>();
    static {
        PUBLIC_SUFFIXES.add(".css");
        PUBLIC_SUFFIXES.add(".js");
        PUBLIC_SUFFIXES.add(".png");
        PUBLIC_SUFFIXES.add(".jpg");
        PUBLIC_SUFFIXES.add(".jpeg");
        PUBLIC_SUFFIXES.add(".gif");
        PUBLIC_SUFFIXES.add(".ico");
        PUBLIC_SUFFIXES.add(".woff");
        PUBLIC_SUFFIXES.add(".woff2");
        PUBLIC_SUFFIXES.add(".ttf");
    }

    /** 不需要登录的路径前缀 */
    private static final Set<String> PUBLIC_PATHS = new HashSet<>();
    static {
        PUBLIC_PATHS.add("/login.jsp");
        PUBLIC_PATHS.add("/register.jsp");
        PUBLIC_PATHS.add("/index.jsp");
        PUBLIC_PATHS.add("/error.jsp");
        PUBLIC_PATHS.add("/login");
        PUBLIC_PATHS.add("/register");
        PUBLIC_PATHS.add("/logout");
        PUBLIC_PATHS.add("/verifyCode");
        PUBLIC_PATHS.add("/checkUnique");
        PUBLIC_PATHS.add("/region");
        PUBLIC_PATHS.add("/");
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        String path = uri.substring(ctx.length());

        // 静态资源直接放行
        for (String suffix : PUBLIC_SUFFIXES) {
            if (path.toLowerCase().endsWith(suffix)) {
                chain.doFilter(req, resp);
                return;
            }
        }

        // 公共路径放行
        if (PUBLIC_PATHS.contains(path) || path.startsWith("/css/") || path.startsWith("/js/")
                || path.startsWith("/images/") || path.startsWith("/upload/")) {
            chain.doFilter(req, resp);
            return;
        }

        // 检查 session
        HttpSession session = request.getSession(false);
        User user = (session == null) ? null : (User) session.getAttribute("loginUser");

        if (user == null) {
            // 检查记住我 cookie
            User remembered = RememberMeHelper.getRememberedUser(request);
            if (remembered != null) {
                session = request.getSession();
                session.setAttribute("loginUser", remembered);
                chain.doFilter(req, resp);
                return;
            }
            // 未登录跳转
            response.sendRedirect(ctx + "/login.jsp");
            return;
        }

        chain.doFilter(req, resp);
    }

    @Override
    public void destroy() {}
}
