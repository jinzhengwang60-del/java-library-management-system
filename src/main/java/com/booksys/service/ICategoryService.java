package com.booksys.service;

import com.booksys.entity.Category;
import java.util.List;

public interface ICategoryService {
    List<Category> findAll();
    Category findById(Integer id);
    boolean addCategory(Category c);
    boolean updateCategory(Category c);
    boolean deleteCategory(Integer id);
}
