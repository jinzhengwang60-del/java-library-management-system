package com.booksys.servlet;

import com.booksys.entity.User;
import com.booksys.service.IUserService;
import com.booksys.service.impl.UserServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private final IUserService userService = new UserServiceImpl();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String realName = request.getParameter("realName");
        String gender = request.getParameter("gender");
        String province = request.getParameter("province");
        String city = request.getParameter("city");
        String district = request.getParameter("district");
        String address = request.getParameter("address");

        // 服务端二次校验
        if (username == null || username.length() < 3) {
            forwardError(request, response, "用户名长度至少3位");
            return;
        }
        if (password == null || password.length() < 6) {
            forwardError(request, response, "密码长度至少6位");
            return;
        }
        if (userService.existsByUsername(username)) {
            forwardError(request, response, "用户名已被占用");
            return;
        }
        if (email != null && !email.isEmpty() && userService.existsByEmail(email)) {
            forwardError(request, response, "邮箱已被注册");
            return;
        }
        if (phone != null && !phone.isEmpty() && userService.existsByPhone(phone)) {
            forwardError(request, response, "手机号已被注册");
            return;
        }

        User u = new User();
        u.setUsername(username);
        u.setPassword(password);
        u.setRealName(realName);
        u.setEmail(email);
        u.setPhone(phone);
        u.setGender(gender);
        u.setProvince(province);
        u.setCity(city);
        u.setDistrict(district);
        u.setAddress(address);

        if (userService.register(u)) {
            request.setAttribute("msg", "注册成功，请登录");
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        } else {
            forwardError(request, response, "注册失败，请重试");
        }
    }

    private void forwardError(HttpServletRequest req, HttpServletResponse resp, String msg)
            throws ServletException, IOException {
        req.setAttribute("error", msg);
        req.getRequestDispatcher("/register.jsp").forward(req, resp);
    }
}
