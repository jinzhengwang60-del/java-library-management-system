package com.booksys.dao.impl;

import com.booksys.dao.ICategoryDao;
import com.booksys.entity.Category;
import com.booksys.util.DbUtil;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import java.sql.SQLException;
import java.util.List;

public class CategoryDaoImpl implements ICategoryDao {

    private static final String SELECT_BASE =
            "SELECT id, name, description, create_time AS createTime FROM t_category";

    private final QueryRunner qr = DbUtil.getQueryRunner();

    @Override
    public int insert(Category c) {
        try {
            return qr.update("INSERT INTO t_category(name,description) VALUES(?,?)",
                    c.getName(), c.getDescription());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int deleteById(Integer id) {
        try {
            return qr.update("DELETE FROM t_category WHERE id=?", id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int update(Category c) {
        try {
            return qr.update("UPDATE t_category SET name=?, description=? WHERE id=?",
                    c.getName(), c.getDescription(), c.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Category findById(Integer id) {
        try {
            return qr.query(SELECT_BASE + " WHERE id=?", new BeanHandler<>(Category.class), id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Category> findAll() {
        try {
            return qr.query(SELECT_BASE + " ORDER BY id", new BeanListHandler<>(Category.class));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
