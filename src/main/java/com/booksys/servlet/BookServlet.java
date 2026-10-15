package com.booksys.servlet;

import com.booksys.entity.Book;
import com.booksys.util.FormValues;
import com.booksys.util.ImageStore;
import com.booksys.entity.Category;
import com.booksys.entity.PageBean;
import com.booksys.entity.User;
import com.booksys.service.IBookService;
import com.booksys.service.ICategoryService;
import com.booksys.service.impl.BookServiceImpl;
import com.booksys.service.impl.CategoryServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

/**
 * 图书 Servlet（多路由通过 PathInfo 区分）
 *  GET   /book/list?keyword=xxx&categoryId=1&pageNum=1
 *  GET   /book/detail?id=1
 *  GET   /book/edit?id=1   -> 跳转编辑页
 *  POST  /book/save        -> 新增或更新
 *  POST  /book/delete?id=1
 *  GET   /book/top         -> 排行榜数据
 */
@WebServlet("/book/*")
@MultipartConfig(
        maxFileSize = 5 * 1024 * 1024,         // 5MB
        maxRequestSize = 10 * 1024 * 1024,     // 10MB
        fileSizeThreshold = 1024 * 1024
)
public class BookServlet extends HttpServlet {

    private final IBookService bookService = new BookServiceImpl();
    private final ICategoryService categoryService = new CategoryServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo();
        if (path == null) path = "/list";

        switch (path) {
            case "/list":
                list(request, response);
                break;
            case "/detail":
                detail(request, response);
                break;
            case "/edit":
                edit(request, response);
                break;
            case "/add":
                addForm(request, response);
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/book/list");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo();
        if ("/save".equals(path)) {
            save(request, response);
        } else if ("/delete".equals(path)) {
            delete(request, response);
        } else {
            doGet(request, response);
        }
    }

    /** 列表页 */
    private void list(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String keyword = request.getParameter("keyword");
        String cidStr = request.getParameter("categoryId");
        String pageStr = request.getParameter("pageNum");

        Integer categoryId = null;
        if (cidStr != null && !cidStr.isEmpty()) {
            try { categoryId = Integer.parseInt(cidStr); } catch (NumberFormatException ignored) {}
        }
        int pageNum = 1;
        if (pageStr != null && !pageStr.isEmpty()) {
            try { pageNum = Integer.parseInt(pageStr); } catch (NumberFormatException ignored) {}
        }

        PageBean<Book> pb = bookService.findByPage(pageNum, 8, keyword, categoryId);
        List<Category> categories = categoryService.findAll();
        List<Book> topBooks = bookService.getTopByClick(5);

        request.setAttribute("pageBean", pb);
        request.setAttribute("categories", categories);
        request.setAttribute("keyword", keyword);
        request.setAttribute("selectedCategoryId", categoryId);
        request.setAttribute("topBooks", topBooks);
        request.getRequestDispatcher("/WEB-INF/views/book_list.jsp").forward(request, response);
    }

    /** 详情页 */
    private void detail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id = parseId(request.getParameter("id"));
        if (id == null) { response.sendRedirect(request.getContextPath() + "/book/list"); return; }
        Book book = bookService.viewDetail(id);
        if (book == null) { response.sendRedirect(request.getContextPath() + "/book/list"); return; }
        request.setAttribute("book", book);
        request.getRequestDispatcher("/WEB-INF/views/book_detail.jsp").forward(request, response);
    }

    /** 新增表单 */
    private void addForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!checkAdmin(request, response)) return;
        request.setAttribute("categories", categoryService.findAll());
        request.getRequestDispatcher("/WEB-INF/views/book_edit.jsp").forward(request, response);
    }

    /** 编辑表单 */
    private void edit(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!checkAdmin(request, response)) return;
        Integer id = parseId(request.getParameter("id"));
        if (id == null) { response.sendRedirect(request.getContextPath() + "/book/list"); return; }
        Book book = bookService.findById(id);
        request.setAttribute("book", book);
        request.setAttribute("categories", categoryService.findAll());
        request.getRequestDispatcher("/WEB-INF/views/book_edit.jsp").forward(request, response);
    }

    /** 新增/更新；校验失败不写库，版本不一致不覆盖其他请求的修改。 */
    private void save(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!checkAdmin(request, response)) return;
        String uploaded = null;
        try {
            Book book = FormValues.book(request);
            Book previous = book.getId() == null ? null : bookService.findById(book.getId());
            if (book.getId() != null && previous == null) { response.sendError(404); return; }
            if (previous != null) {
                String version = request.getParameter("version");
                if (version == null || !version.matches("[0-9]{1,10}"))
                    throw new IllegalArgumentException("编辑页面已过期，请刷新后重试");
                try { book.setVersion(Integer.parseInt(version)); }
                catch (NumberFormatException e) { throw new IllegalArgumentException("编辑页面已过期，请刷新后重试"); }
                book.setCover(previous.getCover());
            }
            if (book.getCategoryId() != null && categoryService.findAll().stream()
                    .noneMatch(category -> book.getCategoryId().equals(category.getId())))
                throw new IllegalArgumentException("图书分类不存在");
            uploaded = ImageStore.upload(request, "coverFile", 5L * 1024 * 1024);
            if (uploaded != null) book.setCover(uploaded);
            if (book.getCover() == null) book.setCover("/images/default-book.png");
            boolean saved = book.getId() == null ? bookService.addBook(book) : bookService.updateBook(book);
            if (!saved) {
                ImageStore.discard(uploaded);
                response.sendError(409, "图书信息已变化，请刷新编辑页面后重试");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/book/list");
        } catch (IllegalArgumentException e) {
            ImageStore.discard(uploaded);
            response.sendError(400, e.getMessage());
        } catch (RuntimeException | IOException e) {
            ImageStore.discard(uploaded);
            throw e;
        }
    }

    /** 删除 */
    private void delete(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (!checkAdmin(request, response)) return;
        Integer id = parseId(request.getParameter("id"));
        if (id != null) {
            bookService.deleteBook(id);
        }
        response.sendRedirect(request.getContextPath() + "/book/list");
    }



    private boolean checkAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User u = (session == null) ? null : (User) session.getAttribute("loginUser");
        if (u == null || u.getRole() == null || u.getRole() != 1) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "需要管理员权限");
            return false;
        }
        return true;
    }

    private Integer parseId(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
    }
}
