package com.booksys.service.impl;

import com.booksys.dao.IBookDao;
import com.booksys.dao.impl.BookDaoImpl;
import com.booksys.entity.Book;
import com.booksys.entity.PageBean;
import com.booksys.service.IBookService;

import java.util.List;

public class BookServiceImpl implements IBookService {

    private final IBookDao bookDao = new BookDaoImpl();

    @Override
    public PageBean<Book> findByPage(int pageNum, int pageSize, String keyword, Integer categoryId) {
        if (pageNum < 1) pageNum = 1;
        int total = bookDao.countByCondition(keyword, categoryId);
        PageBean<Book> pb = new PageBean<>(pageNum, pageSize, total);
        List<Book> list = bookDao.findByPage(keyword, categoryId, (pageNum - 1) * pageSize, pageSize);
        pb.setList(list);
        return pb;
    }

    @Override
    public Book findById(Integer id) {
        return bookDao.findById(id);
    }

    @Override
    public Book viewDetail(Integer id) {
        bookDao.incrementClickCount(id);
        return bookDao.findById(id);
    }

    @Override
    public boolean addBook(Book book) {
        return bookDao.insert(book) > 0;
    }

    @Override
    public boolean updateBook(Book book) {
        return bookDao.update(book) > 0;
    }

    @Override
    public boolean deleteBook(Integer id) {
        return bookDao.deleteById(id) > 0;
    }

    @Override
    public List<Book> getTopByClick(int limit) {
        return bookDao.findTopByClickCount(limit);
    }
}
