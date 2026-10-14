package com.booksys.dao;

import com.booksys.entity.User;
import java.util.List;

/**
 * 用户 DAO 接口
 */
public interface IUserDao {

    /** 新增用户 */
    int insert(User user);

    /** 根据ID删除 */
    int deleteById(Integer id);

    /** 更新用户 */
    int update(User user);

    /** 根据ID查询 */
    User findById(Integer id);

    /** 根据用户名查询 */
    User findByUsername(String username);

    /** 用于登录校验 */
    User findByUsernameAndPassword(String username, String password);

    /** 校验用户名是否已存在 */
    boolean existsByUsername(String username);

    /** 校验邮箱是否已存在 */
    boolean existsByEmail(String email);

    /** 校验手机号是否已存在 */
    boolean existsByPhone(String phone);

    /** 分页查询用户 */
    List<User> findByPage(int offset, int limit);

    /** 用户总数 */
    int count();
}
