package com.booksys.servlet;

import com.booksys.filter.RememberMeHelper;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(405, "请使用页面上的退出按钮");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        RememberMeHelper.clearRememberMe(request, response);
        response.sendRedirect(request.getContextPath() + "/login.jsp");
    }
}
