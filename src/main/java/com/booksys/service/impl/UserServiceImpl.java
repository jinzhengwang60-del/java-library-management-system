package com.booksys.service.impl;

import com.booksys.dao.IUserDao;
import com.booksys.dao.impl.UserDaoImpl;
import com.booksys.entity.PageBean;
import com.booksys.util.DatabaseErrors;
import com.booksys.entity.User;
import com.booksys.service.IUserService;
import com.booksys.util.PasswordUtil;

import java.util.List;

public class UserServiceImpl implements IUserService {

    private final IUserDao userDao = new UserDaoImpl();

    @Override
    public boolean register(User user) {
        if (userDao.existsByUsername(user.getUsername())) return false;
        // 密码加密
        user.setPassword(PasswordUtil.hash(user.getPassword()));
        user.setRole(0);
        user.setStatus(1);
        try { return userDao.insert(user) > 0; }
        catch (RuntimeException e) { if (DatabaseErrors.duplicateKey(e)) return false; throw e; }
    }

    @Override
    public User login(String username, String password) {
        if (username == null || password == null) return null;
        User user = userDao.findByUsername(username.trim());
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())
                || !PasswordUtil.verify(password, user.getPassword())) return null;
        if (PasswordUtil.needsUpgrade(user.getPassword())) {
            String previous = user.getPassword();
            String upgraded = PasswordUtil.hash(password);
            if (userDao.updatePasswordIfMatch(user.getId(), previous, upgraded) == 1) {
                user.setPassword(upgraded);
            } else {
                user = userDao.findById(user.getId());
                if (user == null || !Integer.valueOf(1).equals(user.getStatus())
                        || !PasswordUtil.verify(password, user.getPassword())) return null;
            }
        }
        return user;
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
        try { return userDao.update(user) > 0; }
        catch (RuntimeException e) {
            if (DatabaseErrors.duplicateKey(e)) throw new IllegalArgumentException("邮箱或手机号码已被使用");
            throw e;
        }
    }

    @Override
    public PageBean<User> findByPage(int pageNum, int pageSize) {
        if (pageNum < 1) pageNum = 1;
        int total = userDao.count();
        PageBean<User> pb = new PageBean<>(pageNum, pageSize, total);
        List<User> list = userDao.findByPage(pb.getOffset(), pb.getPageSize());
        pb.setList(list);
        return pb;
    }

    @Override
    public boolean deleteUser(Integer id) {
        return userDao.deleteById(id) > 0;
    }
}
