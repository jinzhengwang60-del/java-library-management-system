package com.booksys.service.impl;

import com.booksys.dao.ICategoryDao;
import com.booksys.dao.impl.CategoryDaoImpl;
import com.booksys.entity.Category;
import com.booksys.service.ICategoryService;

import java.util.List;

public class CategoryServiceImpl implements ICategoryService {

    private final ICategoryDao dao = new CategoryDaoImpl();

    @Override
    public List<Category> findAll() {
        return dao.findAll();
    }

    @Override
    public Category findById(Integer id) {
        return dao.findById(id);
    }

    @Override
    public boolean addCategory(Category c) {
        return dao.insert(c) > 0;
    }

    @Override
    public boolean updateCategory(Category c) {
        return dao.update(c) > 0;
    }

    @Override
    public boolean deleteCategory(Integer id) {
        return dao.deleteById(id) > 0;
    }
}
