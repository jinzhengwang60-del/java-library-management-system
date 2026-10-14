package com.booksys.servlet;

import com.booksys.util.VerifyCodeUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 验证码 Servlet
 * 任务书要求：图片验证码
 */
@WebServlet("/verifyCode")
public class VerifyCodeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        String code = VerifyCodeUtil.generateCode();
        HttpSession session = request.getSession();
        session.setAttribute("verifyCode", code);

        VerifyCodeUtil.writeImage(code, response.getOutputStream());
    }
}
