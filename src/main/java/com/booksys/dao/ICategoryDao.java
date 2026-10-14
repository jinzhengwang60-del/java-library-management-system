package com.booksys.dao;

import com.booksys.entity.Category;
import java.util.List;

public interface ICategoryDao {
    int insert(Category c);
    int deleteById(Integer id);
    int update(Category c);
    Category findById(Integer id);
    List<Category> findAll();
}
