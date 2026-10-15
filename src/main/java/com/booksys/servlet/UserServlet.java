package com.booksys.servlet;

import com.booksys.entity.PageBean;
import com.booksys.entity.User;
import com.booksys.util.FormValues;
import com.booksys.util.ImageStore;
import com.booksys.service.IUserService;
import com.booksys.service.impl.UserServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 用户相关
 *  GET  /user/profile      个人中心
 *  POST /user/update       修改信息
 *  GET  /user/list         用户列表（管理员）
 *  POST /user/delete?id=1  删除用户（管理员）
 */
@WebServlet("/user/*")
@MultipartConfig(maxFileSize = 2 * 1024 * 1024, maxRequestSize = 5 * 1024 * 1024)
public class UserServlet extends HttpServlet {

    private final IUserService userService = new UserServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo();
        if (path == null) path = "/profile";
        switch (path) {
            case "/profile": profile(request, response); break;
            case "/list":    list(request, response); break;
            default:         response.sendRedirect(request.getContextPath() + "/user/profile");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo();
        if ("/update".equals(path)) {
            update(request, response);
        } else if ("/delete".equals(path)) {
            delete(request, response);
        } else {
            doGet(request, response);
        }
    }

    private void profile(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User u = (session == null) ? null : (User) session.getAttribute("loginUser");
        if (u == null) { response.sendRedirect(request.getContextPath() + "/login.jsp"); return; }
        User fresh = userService.findById(u.getId());
        request.setAttribute("user", fresh);
        request.getRequestDispatcher("/WEB-INF/views/user_profile.jsp").forward(request, response);
    }

    private void update(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User current = session == null ? null : (User) session.getAttribute("loginUser");
        if (current == null) { response.sendRedirect(request.getContextPath() + "/login.jsp"); return; }
        String uploaded = null;
        try {
            User edit = userService.findById(current.getId());
            if (edit == null) { response.sendError(404); return; }
            String email = FormValues.text(request.getParameter("email"), "邮箱", 100, false);
            String phone = FormValues.text(request.getParameter("phone"), "手机", 20, false);
            if (email != null && !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
                throw new IllegalArgumentException("邮箱格式无效");
            if (phone != null && !phone.matches("[0-9+() -]{3,20}"))
                throw new IllegalArgumentException("手机号码格式无效");
            if (email != null && !java.util.Objects.equals(email, edit.getEmail()) && userService.existsByEmail(email))
                throw new IllegalArgumentException("邮箱已被使用");
            if (phone != null && !java.util.Objects.equals(phone, edit.getPhone()) && userService.existsByPhone(phone))
                throw new IllegalArgumentException("手机号码已被使用");
            edit.setRealName(FormValues.text(request.getParameter("realName"), "姓名", 50, false));
            edit.setEmail(email); edit.setPhone(phone);
            edit.setGender(FormValues.text(request.getParameter("gender"), "性别", 10, false));
            edit.setProvince(FormValues.text(request.getParameter("province"), "省份", 50, false));
            edit.setCity(FormValues.text(request.getParameter("city"), "城市", 50, false));
            edit.setDistrict(FormValues.text(request.getParameter("district"), "区县", 50, false));
            edit.setAddress(FormValues.text(request.getParameter("address"), "地址", 200, false));
            uploaded = ImageStore.upload(request, "avatarFile", 2L * 1024 * 1024);
            if (uploaded != null) edit.setAvatar(uploaded);
            if (!userService.updateUser(edit)) {
                ImageStore.discard(uploaded);
                response.sendError(409, "个人资料已变化，请刷新后重试");
                return;
            }
            session.setAttribute("loginUser", edit);
            session.setAttribute("flashMsg", "更新成功");
            response.sendRedirect(request.getContextPath() + "/user/profile");
        } catch (IllegalArgumentException e) {
            ImageStore.discard(uploaded);
            response.sendError(400, e.getMessage());
        } catch (RuntimeException | IOException e) {
            ImageStore.discard(uploaded);
            throw e;
        }
    }

    private void list(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User u = (session == null) ? null : (User) session.getAttribute("loginUser");
        if (u == null || u.getRole() == null || u.getRole() != 1) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        int pageNum = 1;
        try {
            String p = request.getParameter("pageNum");
            if (p != null) pageNum = Integer.parseInt(p);
        } catch (NumberFormatException ignored) {}
        PageBean<User> pb = userService.findByPage(pageNum, 10);
        request.setAttribute("pageBean", pb);
        request.getRequestDispatcher("/WEB-INF/views/user_list.jsp").forward(request, response);
    }

    private void delete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User u = (session == null) ? null : (User) session.getAttribute("loginUser");
        if (u == null || u.getRole() == null || u.getRole() != 1) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            Integer id = Integer.parseInt(request.getParameter("id"));
            if (!id.equals(u.getId())) {
                if (!userService.deleteUser(id)) {
                    response.sendError(409, "管理员账号不能停用，或该账号已停用");
                    return;
                }
            }
        } catch (NumberFormatException ignored) {}
        response.sendRedirect(request.getContextPath() + "/user/list");
    }
}
