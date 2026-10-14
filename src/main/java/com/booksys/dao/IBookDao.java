package com.booksys.dao;

import com.booksys.entity.Book;
import java.util.List;

/**
 * 图书 DAO 接口
 */
public interface IBookDao {

    int insert(Book book);

    int deleteById(Integer id);

    int update(Book book);

    Book findById(Integer id);

    /** 分页 + 多条件查询（关键字、分类） */
    List<Book> findByPage(String keyword, Integer categoryId, int offset, int limit);

    /** 满足条件的总记录数 */
    int countByCondition(String keyword, Integer categoryId);

    /** 增加点击量 */
    int incrementClickCount(Integer id);

    /** 按点击量排行 TOP N */
    List<Book> findTopByClickCount(int limit);

    /** 减库存（借书时） */
    int decreaseStock(Integer id);

    /** 加库存（还书时） */
    int increaseStock(Integer id);
}
