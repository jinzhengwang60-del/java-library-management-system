package com.booksys.util;

import com.booksys.entity.Book;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class FormValues {
    private FormValues() {}

    public static String text(String value, String label, int max, boolean required) {
        String text = value == null ? null : value.trim();
        if (text == null || text.isEmpty()) {
            if (required) throw new IllegalArgumentException(label + "不能为空");
            return null;
        }
        if (text.length() > max) throw new IllegalArgumentException(label + "过长");
        return text;
    }

    public static Integer id(String value, String label) {
        String text = text(value, label, 10, false);
        if (text == null) return null;
        try {
            int id = Integer.parseInt(text);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) { throw new IllegalArgumentException(label + "必须是正整数"); }
    }

    public static Book book(HttpServletRequest request) {
        Book book = new Book();
        book.setId(id(request.getParameter("id"), "图书编号"));
        book.setBookNo(text(request.getParameter("bookNo"), "馆藏编号", 50, true));
        book.setTitle(text(request.getParameter("title"), "书名", 100, true));
        book.setAuthor(text(request.getParameter("author"), "作者", 50, true));
        book.setPublisher(text(request.getParameter("publisher"), "出版社", 100, false));
        book.setIsbn(text(request.getParameter("isbn"), "ISBN", 30, false));
        book.setDescription(text(request.getParameter("description"), "简介", 10000, false));
        book.setCategoryId(id(request.getParameter("categoryId"), "分类编号"));
        String date = text(request.getParameter("publishDate"), "出版日期", 10, false);
        if (date != null) {
            try { book.setPublishDate(java.sql.Timestamp.valueOf(LocalDate.parse(date).atStartOfDay())); }
            catch (DateTimeParseException e) { throw new IllegalArgumentException("出版日期格式无效"); }
        }
        try {
            String price = text(request.getParameter("price"), "价格", 20, false);
            BigDecimal amount = price == null ? BigDecimal.ZERO : new BigDecimal(price);
            if (amount.signum() < 0 || amount.compareTo(new BigDecimal("99999999.99")) > 0 || amount.scale() > 2)
                throw new NumberFormatException();
            book.setPrice(amount);
        } catch (NumberFormatException e) { throw new IllegalArgumentException("价格必须是有效的非负金额"); }
        try {
            String stock = text(request.getParameter("stock"), "库存", 10, false);
            int count = stock == null ? 0 : Integer.parseInt(stock);
            if (count < 0) throw new NumberFormatException();
            book.setStock(count);
        } catch (NumberFormatException e) { throw new IllegalArgumentException("库存必须是非负整数"); }
        book.setStatus(1);
        return book;
    }
}
