package com.booksys.dao;

import com.booksys.entity.Borrow;
import java.util.List;

public interface IBorrowDao {
    int insert(Borrow b);
    int updateReturn(Integer id);
    Borrow findById(Integer id);

    /** 用户的借阅历史 */
    List<Borrow> findByUserId(Integer userId, int offset, int limit);
    int countByUserId(Integer userId);

    /** 所有借阅记录（管理员） */
    List<Borrow> findAll(int offset, int limit);
    int countAll();

    /** 用户是否已借阅某本书且未归还 */
    boolean hasUnreturned(Integer userId, Integer bookId);
}
