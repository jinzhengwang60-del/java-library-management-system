package com.booksys.dao.impl;

import com.booksys.dao.IBorrowDao;
import com.booksys.entity.Borrow;
import com.booksys.util.DbUtil;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import java.sql.SQLException;
import java.util.List;

public class BorrowDaoImpl implements IBorrowDao {

    private static final String SELECT_BASE =
            "SELECT br.id, br.user_id AS userId, br.book_id AS bookId, " +
                    "br.borrow_date AS borrowDate, br.due_date AS dueDate, br.return_date AS returnDate, br.status, " +
                    "u.username, b.title AS bookTitle, b.cover AS bookCover " +
                    "FROM t_borrow br " +
                    "LEFT JOIN t_user u ON br.user_id = u.id " +
                    "LEFT JOIN t_book b ON br.book_id = b.id ";

    private final QueryRunner qr = DbUtil.getQueryRunner();

    @Override
    public int insert(Borrow b) {
        try {
            return qr.update("INSERT INTO t_borrow(user_id,book_id,borrow_date,due_date,status) VALUES(?,?,?,?,0)",
                    b.getUserId(), b.getBookId(), b.getBorrowDate(), b.getDueDate());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int updateReturn(Integer id) {
        try {
            return qr.update("UPDATE t_borrow SET return_date=NOW(), status=1 WHERE id=? AND status=0", id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Borrow findById(Integer id) {
        try {
            return qr.query(SELECT_BASE + " WHERE br.id=?", new BeanHandler<>(Borrow.class), id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Borrow> findByUserId(Integer userId, int offset, int limit) {
        try {
            return qr.query(SELECT_BASE + " WHERE br.user_id=? ORDER BY br.borrow_date DESC LIMIT ?,?",
                    new BeanListHandler<>(Borrow.class), userId, offset, limit);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int countByUserId(Integer userId) {
        try {
            Long c = qr.query("SELECT COUNT(*) FROM t_borrow WHERE user_id=?",
                    new ScalarHandler<>(), userId);
            return c == null ? 0 : c.intValue();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Borrow> findAll(int offset, int limit) {
        try {
            return qr.query(SELECT_BASE + " ORDER BY br.borrow_date DESC LIMIT ?,?",
                    new BeanListHandler<>(Borrow.class), offset, limit);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int countAll() {
        try {
            Long c = qr.query("SELECT COUNT(*) FROM t_borrow", new ScalarHandler<>());
            return c == null ? 0 : c.intValue();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasUnreturned(Integer userId, Integer bookId) {
        try {
            Long c = qr.query("SELECT COUNT(*) FROM t_borrow WHERE user_id=? AND book_id=? AND status=0",
                    new ScalarHandler<>(), userId, bookId);
            return c != null && c > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
