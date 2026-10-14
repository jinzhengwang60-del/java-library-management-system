package com.booksys.servlet;

import com.booksys.entity.Book;
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
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.UUID;

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

    /** 新增/更新 */
    private void save(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!checkAdmin(request, response)) return;

        String idStr = request.getParameter("id");
        Book book = new Book();
        if (idStr != null && !idStr.isEmpty()) {
            try { book.setId(Integer.parseInt(idStr)); } catch (NumberFormatException ignored) {}
        }
        book.setBookNo(request.getParameter("bookNo"));
        book.setTitle(request.getParameter("title"));
        book.setAuthor(request.getParameter("author"));
        book.setPublisher(request.getParameter("publisher"));
        book.setIsbn(request.getParameter("isbn"));
        book.setDescription(request.getParameter("description"));

        String publishDateStr = request.getParameter("publishDate");
        if (publishDateStr != null && !publishDateStr.isEmpty()) {
            try {
                book.setPublishDate(new java.sql.Timestamp(new SimpleDateFormat("yyyy-MM-dd").parse(publishDateStr).getTime()));
            } catch (Exception ignored) {}
        }

        String cid = request.getParameter("categoryId");
        if (cid != null && !cid.isEmpty()) {
            try { book.setCategoryId(Integer.parseInt(cid)); } catch (NumberFormatException ignored) {}
        }

        String priceStr = request.getParameter("price");
        if (priceStr != null && !priceStr.isEmpty()) {
            try { book.setPrice(new BigDecimal(priceStr)); } catch (Exception ignored) {}
        }
        String stockStr = request.getParameter("stock");
        if (stockStr != null && !stockStr.isEmpty()) {
            try { book.setStock(Integer.parseInt(stockStr)); } catch (NumberFormatException ignored) {}
        }
        book.setStatus(1);

        // 文件上传
        Part filePart = null;
        try {
            filePart = request.getPart("coverFile");
        } catch (Exception ignored) {}

        if (filePart != null && filePart.getSize() > 0) {
            String savedPath = saveUpload(request, filePart);
            if (savedPath != null) book.setCover(savedPath);
        } else if (book.getId() != null) {
            // 编辑时保留原 cover
            Book old = bookService.findById(book.getId());
            if (old != null) book.setCover(old.getCover());
        }
        if (book.getCover() == null) book.setCover("/images/default-book.png");

        boolean ok;
        if (book.getId() == null) {
            ok = bookService.addBook(book);
        } else {
            ok = bookService.updateBook(book);
        }
        if (ok) {
            response.sendRedirect(request.getContextPath() + "/book/list");
        } else {
            request.setAttribute("error", "保存失败");
            request.setAttribute("book", book);
            request.setAttribute("categories", categoryService.findAll());
            request.getRequestDispatcher("/WEB-INF/views/book_edit.jsp").forward(request, response);
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

    /** 文件保存 */
    private String saveUpload(HttpServletRequest request, Part part) throws IOException {
        String originalName = getSubmittedFileName(part);
        if (originalName == null || originalName.isEmpty()) return null;
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot > -1) ext = originalName.substring(dot);
        String newName = UUID.randomUUID().toString().replace("-", "") + ext;

        // 保存到 webapp/upload 目录（运行时即 Tomcat 部署目录下的 upload）
        String uploadDir = request.getServletContext().getRealPath("/upload");
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();
        File target = new File(dir, newName);
        part.write(target.getAbsolutePath());
        return "/upload/" + newName;
    }

    private String getSubmittedFileName(Part part) {
        String cd = part.getHeader("content-disposition");
        if (cd == null) return null;
        for (String token : cd.split(";")) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
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
