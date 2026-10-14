package com.booksys.service.impl;

import com.booksys.dao.IBorrowDao;
import com.booksys.dao.impl.BorrowDaoImpl;
import com.booksys.entity.Book;
import com.booksys.entity.Borrow;
import com.booksys.entity.PageBean;
import com.booksys.service.IBorrowService;
import com.booksys.util.DbUtil;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;

public class BorrowServiceImpl implements IBorrowService {
    private final IBorrowDao borrowDao = new BorrowDaoImpl();
    private static final int BORROW_DAYS = 30;

    @Override
    public String borrowBook(Integer userId, Integer bookId) {
        try (Connection connection = DbUtil.getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            QueryRunner runner = new QueryRunner();
            try {
                // Serialize competing borrowers of the same book, including the last copy.
                Book book = runner.query(connection,
                        "SELECT id, stock FROM t_book WHERE id=? FOR UPDATE",
                        new BeanHandler<>(Book.class), bookId);
                if (book == null) { connection.rollback(); return "图书不存在"; }
                if (book.getStock() == null || book.getStock() <= 0) {
                    connection.rollback(); return "图书已无库存";
                }
                Long active = runner.query(connection,
                        "SELECT COUNT(*) FROM t_borrow WHERE user_id=? AND book_id=? AND status=0",
                        new ScalarHandler<Long>(), userId, bookId);
                if (active != null && active > 0) {
                    connection.rollback(); return "您已借阅此书且未归还";
                }
                Timestamp now = new Timestamp(System.currentTimeMillis());
                Calendar due = Calendar.getInstance();
                due.setTime(now);
                due.add(Calendar.DAY_OF_MONTH, BORROW_DAYS);
                int inserted = runner.update(connection,
                        "INSERT INTO t_borrow(user_id,book_id,borrow_date,due_date,status) VALUES(?,?,?,?,0)",
                        userId, bookId, now, new Timestamp(due.getTimeInMillis()));
                int updated = runner.update(connection,
                        "UPDATE t_book SET stock=stock-1 WHERE id=? AND stock>0", bookId);
                if (inserted != 1 || updated != 1) {
                    connection.rollback(); return "借阅失败";
                }
                connection.commit();
                return "借阅成功";
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw new IllegalStateException("Borrow transaction failed", e);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot open borrow transaction", e);
        }
    }

    @Override
    public boolean returnBook(Integer borrowId) {
        try (Connection connection = DbUtil.getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            QueryRunner runner = new QueryRunner();
            try {
                Borrow borrow = runner.query(connection,
                        "SELECT id, book_id AS bookId, status FROM t_borrow WHERE id=? FOR UPDATE",
                        new BeanHandler<>(Borrow.class), borrowId);
                if (borrow == null || Integer.valueOf(1).equals(borrow.getStatus())) {
                    connection.rollback(); return false;
                }
                int returned = runner.update(connection,
                        "UPDATE t_borrow SET return_date=NOW(),status=1 WHERE id=? AND status=0", borrowId);
                int updated = runner.update(connection,
                        "UPDATE t_book SET stock=stock+1 WHERE id=?", borrow.getBookId());
                if (returned != 1 || updated != 1) {
                    connection.rollback(); return false;
                }
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw new IllegalStateException("Return transaction failed", e);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot open return transaction", e);
        }
    }

    @Override
    public PageBean<Borrow> findByUser(Integer userId, int pageNum, int pageSize) {
        if (pageNum < 1) pageNum = 1;
        int total = borrowDao.countByUserId(userId);
        PageBean<Borrow> page = new PageBean<>(pageNum, pageSize, total);
        page.setList(borrowDao.findByUserId(userId, (pageNum - 1) * pageSize, pageSize));
        return page;
    }

    @Override
    public PageBean<Borrow> findAll(int pageNum, int pageSize) {
        if (pageNum < 1) pageNum = 1;
        int total = borrowDao.countAll();
        PageBean<Borrow> page = new PageBean<>(pageNum, pageSize, total);
        List<Borrow> list = borrowDao.findAll((pageNum - 1) * pageSize, pageSize);
        page.setList(list);
        return page;
    }
}
