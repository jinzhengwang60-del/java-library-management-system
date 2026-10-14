package com.booksys.service;

import com.booksys.entity.PageBean;
import com.booksys.entity.User;

public interface IUserService {

    /** 注册 */
    boolean register(User user);

    /** 登录验证（返回完整用户，失败返回null） */
    User login(String username, String password);

    /** 用户名是否已存在 */
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    User findById(Integer id);

    boolean updateUser(User user);

    PageBean<User> findByPage(int pageNum, int pageSize);

    boolean deleteUser(Integer id);
}
