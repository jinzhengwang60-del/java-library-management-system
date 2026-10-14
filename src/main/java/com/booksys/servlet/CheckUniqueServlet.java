package com.booksys.servlet;

import com.booksys.service.IUserService;
import com.booksys.service.impl.UserServiceImpl;
import com.booksys.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Ajax 唯一性校验
 * 任务书要求：注册时校验 ID/Email/Phone 唯一性，采用 Ajax 异步校验
 *
 * 调用方式：
 *   GET /checkUnique?field=username&value=xxx
 *   GET /checkUnique?field=email&value=xxx
 *   GET /checkUnique?field=phone&value=xxx
 *
 * 返回：
 *   {"code":200, "msg":"可用"}   - 可用
 *   {"code":500, "msg":"已存在"}  - 已存在
 */
@WebServlet("/checkUnique")
public class CheckUniqueServlet extends HttpServlet {

    private final IUserService userService = new UserServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        String field = request.getParameter("field");
        String value = request.getParameter("value");

        PrintWriter out = response.getWriter();
        if (field == null || value == null || value.trim().isEmpty()) {
            out.write(JsonUtil.fail("参数错误"));
            return;
        }

        boolean exists = false;
        switch (field) {
            case "username":
                exists = userService.existsByUsername(value);
                break;
            case "email":
                exists = userService.existsByEmail(value);
                break;
            case "phone":
                exists = userService.existsByPhone(value);
                break;
            default:
                out.write(JsonUtil.fail("未知字段"));
                return;
        }

        if (exists) {
            out.write(JsonUtil.fail("已被使用"));
        } else {
            out.write(JsonUtil.success("可用"));
        }
    }
}
