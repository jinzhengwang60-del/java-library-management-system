package com.booksys.dao.impl;

import com.booksys.dao.IBookDao;
import com.booksys.entity.Book;
import com.booksys.util.DbUtil;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 图书 DAO 实现
 */
public class BookDaoImpl implements IBookDao {

    // 关联查询，带分类名
    private static final String SELECT_BASE =
            "SELECT b.id, b.book_no AS bookNo, b.title, b.author, b.publisher, b.publish_date AS publishDate, " +
                    "b.isbn, b.category_id AS categoryId, c.name AS categoryName, b.price, b.stock, " +
                    "b.cover, b.description, b.click_count AS clickCount, b.status, b.create_time AS createTime " +
                    "FROM t_book b LEFT JOIN t_category c ON b.category_id = c.id ";

    private final QueryRunner qr = DbUtil.getQueryRunner();

    @Override
    public int insert(Book book) {
        String sql = "INSERT INTO t_book(book_no,title,author,publisher,publish_date,isbn,category_id,price,stock,cover,description,status) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            return qr.update(sql,
                    book.getBookNo(), book.getTitle(), book.getAuthor(), book.getPublisher(),
                    book.getPublishDate(), book.getIsbn(), book.getCategoryId(),
                    book.getPrice(), book.getStock(),
                    book.getCover() == null ? "/images/default-book.png" : book.getCover(),
                    book.getDescription(),
                    book.getStatus() == null ? 1 : book.getStatus());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int deleteById(Integer id) {
        try {
            return qr.update("DELETE FROM t_book WHERE id=?", id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int update(Book book) {
        String sql = "UPDATE t_book SET book_no=?,title=?,author=?,publisher=?,publish_date=?,isbn=?,category_id=?,price=?,stock=?,cover=?,description=?,status=? WHERE id=?";
        try {
            return qr.update(sql,
                    book.getBookNo(), book.getTitle(), book.getAuthor(), book.getPublisher(),
                    book.getPublishDate(), book.getIsbn(), book.getCategoryId(),
                    book.getPrice(), book.getStock(), book.getCover(), book.getDescription(),
                    book.getStatus(), book.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Book findById(Integer id) {
        try {
            return qr.query(SELECT_BASE + " WHERE b.id=?", new BeanHandler<>(Book.class), id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Book> findByPage(String keyword, Integer categoryId, int offset, int limit) {
        StringBuilder sql = new StringBuilder(SELECT_BASE).append(" WHERE b.status=1 ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (b.title LIKE ? OR b.author LIKE ? OR b.isbn LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (categoryId != null) {
            sql.append(" AND b.category_id=? ");
            params.add(categoryId);
        }
        sql.append(" ORDER BY b.id DESC LIMIT ?,? ");
        params.add(offset);
        params.add(limit);

        try {
            return qr.query(sql.toString(), new BeanListHandler<>(Book.class), params.toArray());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int countByCondition(String keyword, Integer categoryId) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM t_book b WHERE b.status=1 ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (b.title LIKE ? OR b.author LIKE ? OR b.isbn LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (categoryId != null) {
            sql.append(" AND b.category_id=? ");
            params.add(categoryId);
        }

        try {
            Long c = qr.query(sql.toString(), new ScalarHandler<>(), params.toArray());
            return c == null ? 0 : c.intValue();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int incrementClickCount(Integer id) {
        try {
            return qr.update("UPDATE t_book SET click_count=click_count+1 WHERE id=?", id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Book> findTopByClickCount(int limit) {
        try {
            return qr.query(SELECT_BASE + " WHERE b.status=1 ORDER BY b.click_count DESC LIMIT ?",
                    new BeanListHandler<>(Book.class), limit);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int decreaseStock(Integer id) {
        try {
            return qr.update("UPDATE t_book SET stock=stock-1 WHERE id=? AND stock>0", id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int increaseStock(Integer id) {
        try {
            return qr.update("UPDATE t_book SET stock=stock+1 WHERE id=?", id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
