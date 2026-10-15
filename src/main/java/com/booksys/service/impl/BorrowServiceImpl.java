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
import java.sql.Timestamp;
import java.util.Calendar;

public class BorrowServiceImpl implements IBorrowService {
    private final IBorrowDao borrowDao = new BorrowDaoImpl();
    private static final int BORROW_DAYS = 30;

    @Override
    public String borrowBook(Integer userId, Integer bookId) {
        return DbUtil.transaction(connection -> {
            QueryRunner runner = new QueryRunner();
            Number status = runner.query(connection, "SELECT status FROM t_user WHERE id=? FOR SHARE",
                    new ScalarHandler<Number>(), userId);
            if (status == null || status.intValue() != 1) return "账号不存在或已停用";
            Book book = runner.query(connection, "SELECT id,stock,status FROM t_book WHERE id=? FOR UPDATE",
                    new BeanHandler<>(Book.class), bookId);
            if (book == null) return "图书不存在";
            if (!Integer.valueOf(1).equals(book.getStatus())) return "图书已下架";
            if (book.getStock() == null || book.getStock() <= 0) return "图书已无库存";
            Long active = runner.query(connection,
                    "SELECT COUNT(*) FROM t_borrow WHERE user_id=? AND book_id=? AND status=0",
                    new ScalarHandler<Long>(), userId, bookId);
            if (active != null && active > 0) return "您已借阅此书且未归还";
            Timestamp now = new Timestamp(System.currentTimeMillis());
            Calendar due = Calendar.getInstance();
            due.setTime(now);
            due.add(Calendar.DAY_OF_MONTH, BORROW_DAYS);
            runner.update(connection,
                    "INSERT INTO t_borrow(user_id,book_id,borrow_date,due_date,status) VALUES(?,?,?,?,0)",
                    userId, bookId, now, new Timestamp(due.getTimeInMillis()));
            int changed = runner.update(connection, "UPDATE t_book SET stock=stock-1,version=version+1 WHERE id=? AND stock>0", bookId);
            if (changed != 1) throw new IllegalStateException("库存更新失败");
            return "借阅成功";
        });
    }

    @Override
    public boolean returnBook(Integer borrowId) {
        return DbUtil.transaction(connection -> {
            QueryRunner runner = new QueryRunner();
            Borrow borrow = runner.query(connection,
                    "SELECT id,book_id AS bookId,status FROM t_borrow WHERE id=? FOR UPDATE",
                    new BeanHandler<>(Borrow.class), borrowId);
            if (borrow == null || Integer.valueOf(1).equals(borrow.getStatus())) return false;
            int returned = runner.update(connection,
                    "UPDATE t_borrow SET return_date=NOW(),status=1 WHERE id=? AND status=0", borrowId);
            int restored = runner.update(connection, "UPDATE t_book SET stock=stock+1,version=version+1 WHERE id=?", borrow.getBookId());
            if (returned != 1 || restored != 1) throw new IllegalStateException("归还记录与库存更新失败");
            return true;
        });
    }

    @Override public PageBean<Borrow> findByUser(Integer userId, int pageNum, int pageSize) {
        PageBean<Borrow> page = new PageBean<>(pageNum, pageSize, borrowDao.countByUserId(userId));
        page.setList(borrowDao.findByUserId(userId, page.getOffset(), page.getPageSize()));
        return page;
    }
    @Override public PageBean<Borrow> findAll(int pageNum, int pageSize) {
        PageBean<Borrow> page = new PageBean<>(pageNum, pageSize, borrowDao.countAll());
        page.setList(borrowDao.findAll(page.getOffset(), page.getPageSize()));
        return page;
    }
}
