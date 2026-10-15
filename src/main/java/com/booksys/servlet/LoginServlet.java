package com.booksys.servlet;

import com.booksys.entity.User;
import com.booksys.filter.RememberMeHelper;
import com.booksys.service.IUserService;
import com.booksys.service.impl.UserServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 登录 Servlet
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final IUserService userService = new UserServiceImpl();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String verifyCode = request.getParameter("verifyCode");
        String remember = request.getParameter("remember");

        HttpSession session = request.getSession();
        String realCode = (String) session.getAttribute("verifyCode");

        // 校验验证码
        if (realCode == null || verifyCode == null || !realCode.equalsIgnoreCase(verifyCode.trim())) {
            request.setAttribute("error", "验证码错误");
            request.setAttribute("username", username);
            request.getRequestDispatcher("/login.jsp").forward(request, response);
            return;
        }

        // 验证码用一次就失效
        session.removeAttribute("verifyCode");

        User user = userService.login(username, password);
        if (user == null) {
            request.setAttribute("error", "用户名或密码错误");
            request.setAttribute("username", username);
            request.getRequestDispatcher("/login.jsp").forward(request, response);
            return;
        }

        request.changeSessionId();
        session.setAttribute("loginUser", user);

        // 记住我
        if ("on".equals(remember) || "true".equals(remember)) {
            RememberMeHelper.setRememberMe(request, response, user);
        } else {
            RememberMeHelper.clearRememberMe(request, response);
        }

        // 跳转
        response.sendRedirect(request.getContextPath() + "/book/list");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
    }
}
