package com.booksys.servlet;

import com.booksys.entity.Borrow;
import com.booksys.entity.PageBean;
import com.booksys.entity.User;
import com.booksys.service.IBorrowService;
import com.booksys.service.impl.BorrowServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 借阅相关
 *  POST /borrow/do?bookId=1     借书
 *  POST /borrow/return?id=1     还书
 *  GET  /borrow/my              我的借阅
 *  GET  /borrow/list            所有借阅（管理员）
 */
@WebServlet("/borrow/*")
public class BorrowServlet extends HttpServlet {

    private final IBorrowService borrowService = new BorrowServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo();
        if (path == null) path = "/my";
        switch (path) {
            case "/my":
                my(request, response);
                break;
            case "/list":
                listAll(request, response);
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/borrow/my");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo();
        if ("/do".equals(path)) {
            doBorrow(request, response);
        } else if ("/return".equals(path)) {
            doReturn(request, response);
        } else {
            doGet(request, response);
        }
    }

    private void doBorrow(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User u = (session == null) ? null : (User) session.getAttribute("loginUser");
        if (u == null) { response.sendRedirect(request.getContextPath() + "/login.jsp"); return; }

        String bookIdStr = request.getParameter("bookId");
        if (bookIdStr == null) { response.sendRedirect(request.getContextPath() + "/book/list"); return; }
        try {
            Integer bookId = Integer.parseInt(bookIdStr);
            String result = borrowService.borrowBook(u.getId(), bookId);
            request.getSession().setAttribute("flashMsg", result);
        } catch (NumberFormatException ignored) {}
        response.sendRedirect(request.getContextPath() + "/borrow/my");
    }

    private void doReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("loginUser");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }
        String idStr = request.getParameter("id");
        if (idStr != null) {
            try {
                Integer id = Integer.parseInt(idStr);
                Borrow record = new com.booksys.dao.impl.BorrowDaoImpl().findById(id);
                if (record == null || (!Integer.valueOf(1).equals(user.getRole())
                        && !user.getId().equals(record.getUserId()))) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                boolean ok = borrowService.returnBook(id);
                request.getSession().setAttribute("flashMsg", ok ? "归还成功" : "归还失败");
            } catch (NumberFormatException ignored) {}
        }
        response.sendRedirect(request.getContextPath() + "/borrow/my");
    }

    private void my(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User u = (session == null) ? null : (User) session.getAttribute("loginUser");
        if (u == null) { response.sendRedirect(request.getContextPath() + "/login.jsp"); return; }

        int pageNum = parseInt(request.getParameter("pageNum"), 1);
        PageBean<Borrow> pb = borrowService.findByUser(u.getId(), pageNum, 10);
        request.setAttribute("pageBean", pb);
        request.getRequestDispatcher("/WEB-INF/views/borrow_my.jsp").forward(request, response);
    }

    private void listAll(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User u = (session == null) ? null : (User) session.getAttribute("loginUser");
        if (u == null || u.getRole() == null || u.getRole() != 1) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        int pageNum = parseInt(request.getParameter("pageNum"), 1);
        PageBean<Borrow> pb = borrowService.findAll(pageNum, 15);
        request.setAttribute("pageBean", pb);
        request.getRequestDispatcher("/WEB-INF/views/borrow_list.jsp").forward(request, response);
    }

    private int parseInt(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }
}
