package com.booksys.servlet;

import com.booksys.entity.PageBean;
import com.booksys.entity.User;
import com.booksys.service.IUserService;
import com.booksys.service.impl.UserServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

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
        User u = (session == null) ? null : (User) session.getAttribute("loginUser");
        if (u == null) { response.sendRedirect(request.getContextPath() + "/login.jsp"); return; }

        User edit = userService.findById(u.getId());
        edit.setRealName(request.getParameter("realName"));
        edit.setEmail(request.getParameter("email"));
        edit.setPhone(request.getParameter("phone"));
        edit.setGender(request.getParameter("gender"));
        edit.setProvince(request.getParameter("province"));
        edit.setCity(request.getParameter("city"));
        edit.setDistrict(request.getParameter("district"));
        edit.setAddress(request.getParameter("address"));

        // 头像上传
        try {
            Part avatarPart = request.getPart("avatarFile");
            if (avatarPart != null && avatarPart.getSize() > 0) {
                String fileName = avatarPart.getSubmittedFileName();
                if (fileName != null && !fileName.isEmpty()) {
                    String ext = "";
                    int dot = fileName.lastIndexOf('.');
                    if (dot > -1) ext = fileName.substring(dot);
                    String newName = UUID.randomUUID().toString().replace("-", "") + ext;
                    String uploadDir = request.getServletContext().getRealPath("/upload");
                    File dir = new File(uploadDir);
                    if (!dir.exists()) dir.mkdirs();
                    avatarPart.write(new File(dir, newName).getAbsolutePath());
                    edit.setAvatar("/upload/" + newName);
                }
            }
        } catch (Exception ignored) {}

        boolean ok = userService.updateUser(edit);
        if (ok) {
            session.setAttribute("loginUser", edit);
            request.getSession().setAttribute("flashMsg", "更新成功");
        } else {
            request.getSession().setAttribute("flashMsg", "更新失败");
        }
        response.sendRedirect(request.getContextPath() + "/user/profile");
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
                userService.deleteUser(id);
            }
        } catch (NumberFormatException ignored) {}
        response.sendRedirect(request.getContextPath() + "/user/list");
    }
}
