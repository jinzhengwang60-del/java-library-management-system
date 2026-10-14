package com.booksys.service.impl;

import com.booksys.dao.IUserDao;
import com.booksys.dao.impl.UserDaoImpl;
import com.booksys.entity.PageBean;
import com.booksys.entity.User;
import com.booksys.service.IUserService;
import com.booksys.util.MD5Util;

import java.util.List;

public class UserServiceImpl implements IUserService {

    private final IUserDao userDao = new UserDaoImpl();

    @Override
    public boolean register(User user) {
        if (userDao.existsByUsername(user.getUsername())) return false;
        // 密码加密
        user.setPassword(MD5Util.encrypt(user.getPassword()));
        user.setRole(0);
        user.setStatus(1);
        return userDao.insert(user) > 0;
    }

    @Override
    public User login(String username, String password) {
        String md5 = MD5Util.encrypt(password);
        return userDao.findByUsernameAndPassword(username, md5);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userDao.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userDao.existsByEmail(email);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return userDao.existsByPhone(phone);
    }

    @Override
    public User findById(Integer id) {
        return userDao.findById(id);
    }

    @Override
    public boolean updateUser(User user) {
        return userDao.update(user) > 0;
    }

    @Override
    public PageBean<User> findByPage(int pageNum, int pageSize) {
        if (pageNum < 1) pageNum = 1;
        int total = userDao.count();
        PageBean<User> pb = new PageBean<>(pageNum, pageSize, total);
        List<User> list = userDao.findByPage((pageNum - 1) * pageSize, pageSize);
        pb.setList(list);
        return pb;
    }

    @Override
    public boolean deleteUser(Integer id) {
        return userDao.deleteById(id) > 0;
    }
}
