package com.booksys.service;

import com.booksys.entity.Borrow;
import com.booksys.entity.PageBean;

public interface IBorrowService {

    /** 借书 */
    String borrowBook(Integer userId, Integer bookId);

    /** 还书 */
    boolean returnBook(Integer borrowId);

    /** 用户借阅记录分页 */
    PageBean<Borrow> findByUser(Integer userId, int pageNum, int pageSize);

    /** 所有借阅记录分页（管理员） */
    PageBean<Borrow> findAll(int pageNum, int pageSize);
}
