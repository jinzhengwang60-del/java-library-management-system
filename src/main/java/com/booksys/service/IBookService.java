package com.booksys.service;

import com.booksys.entity.Book;
import com.booksys.entity.PageBean;
import java.util.List;

public interface IBookService {

    PageBean<Book> findByPage(int pageNum, int pageSize, String keyword, Integer categoryId);

    Book findById(Integer id);

    /** 查看详情时调用，会自增点击数 */
    Book viewDetail(Integer id);

    boolean addBook(Book book);

    boolean updateBook(Book book);

    boolean deleteBook(Integer id);

    /** 排行榜 */
    List<Book> getTopByClick(int limit);
}
