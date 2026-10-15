package com.booksys.service.impl;

import com.booksys.dao.IBookDao;
import com.booksys.dao.impl.BookDaoImpl;
import com.booksys.entity.Book;
import com.booksys.entity.PageBean;
import com.booksys.util.DatabaseErrors;
import com.booksys.service.IBookService;

import java.util.List;

public class BookServiceImpl implements IBookService {

    private final IBookDao bookDao = new BookDaoImpl();

    @Override
    public PageBean<Book> findByPage(int pageNum, int pageSize, String keyword, Integer categoryId) {
        if (pageNum < 1) pageNum = 1;
        int total = bookDao.countByCondition(keyword, categoryId);
        PageBean<Book> pb = new PageBean<>(pageNum, pageSize, total);
        List<Book> list = bookDao.findByPage(keyword, categoryId, pb.getOffset(), pb.getPageSize());
        pb.setList(list);
        return pb;
    }

    @Override
    public Book findById(Integer id) {
        return bookDao.findById(id);
    }

    @Override
    public Book viewDetail(Integer id) {
        Book book = bookDao.findById(id);
        if (book == null || !Integer.valueOf(1).equals(book.getStatus())) return null;
        bookDao.incrementClickCount(id);
        book.setClickCount(book.getClickCount() == null ? 1 : book.getClickCount() + 1);
        return book;
    }

    @Override
    public boolean addBook(Book book) {
        try { return bookDao.insert(book) > 0; }
        catch (RuntimeException e) {
            if (DatabaseErrors.duplicateKey(e)) throw new IllegalArgumentException("馆藏编号已存在");
            throw e;
        }
    }

    @Override
    public boolean updateBook(Book book) {
        try { return bookDao.update(book) > 0; }
        catch (RuntimeException e) {
            if (DatabaseErrors.duplicateKey(e)) throw new IllegalArgumentException("馆藏编号已存在");
            throw e;
        }
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
