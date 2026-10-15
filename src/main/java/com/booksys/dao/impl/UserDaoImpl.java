package com.booksys.dao.impl;

import com.booksys.dao.IUserDao;
import com.booksys.entity.User;
import com.booksys.util.DbUtil;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import java.sql.SQLException;
import java.util.List;

/**
 * 用户 DAO 实现
 * 使用 Apache DBUtils + C3P0 连接池
 */
public class UserDaoImpl implements IUserDao {

    // DBUtils 字段映射要求：通过 setter 注入，列名与属性名匹配（驼峰）
    // 这里在 SQL 中使用 AS 别名，避免数据库下划线列名映射问题

    private static final String SELECT_ALL =
            "SELECT id, username, password, real_name AS realName, email, phone, " +
                    "gender, province, city, district, address, avatar, role, status, create_time AS createTime " +
                    "FROM t_user";

    private final QueryRunner qr = DbUtil.getQueryRunner();

    @Override
    public int insert(User user) {
        String sql = "INSERT INTO t_user(username,password,real_name,email,phone,gender,province,city,district,address,avatar,role,status) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            return qr.update(sql,
                    user.getUsername(), user.getPassword(), user.getRealName(),
                    user.getEmail(), user.getPhone(), user.getGender(),
                    user.getProvince(), user.getCity(), user.getDistrict(), user.getAddress(),
                    user.getAvatar() == null ? "/images/default-avatar.png" : user.getAvatar(),
                    user.getRole() == null ? 0 : user.getRole(),
                    user.getStatus() == null ? 1 : user.getStatus());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int deleteById(Integer id) {
        try {
            return qr.update("UPDATE t_user SET status=0 WHERE id=? AND role<>1 AND status=1", id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int update(User user) {
        String sql = "UPDATE t_user SET real_name=?,email=?,phone=?,gender=?,province=?,city=?,district=?,address=?,avatar=? WHERE id=?";
        try {
            return qr.update(sql,
                    user.getRealName(), user.getEmail(), user.getPhone(), user.getGender(),
                    user.getProvince(), user.getCity(), user.getDistrict(), user.getAddress(),
                    user.getAvatar(), user.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public User findById(Integer id) {
        try {
            return qr.query(SELECT_ALL + " WHERE id=?", new BeanHandler<>(User.class), id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public User findByUsername(String username) {
        try {
            return qr.query(SELECT_ALL + " WHERE username=?", new BeanHandler<>(User.class), username);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int updatePasswordIfMatch(Integer id, String previous, String replacement) {
        try {
            return qr.update("UPDATE t_user SET password=? WHERE id=? AND password=? AND status=1",
                    replacement, id, previous);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public boolean existsByUsername(String username) {
        try {
            Long count = qr.query("SELECT COUNT(*) FROM t_user WHERE username=?",
                    new ScalarHandler<>(), username);
            return count != null && count > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        try {
            Long count = qr.query("SELECT COUNT(*) FROM t_user WHERE email=?",
                    new ScalarHandler<>(), email);
            return count != null && count > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean existsByPhone(String phone) {
        try {
            Long count = qr.query("SELECT COUNT(*) FROM t_user WHERE phone=?",
                    new ScalarHandler<>(), phone);
            return count != null && count > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<User> findByPage(int offset, int limit) {
        try {
            return qr.query(SELECT_ALL + " ORDER BY id DESC LIMIT ?,?",
                    new BeanListHandler<>(User.class), offset, limit);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int count() {
        try {
            Long c = qr.query("SELECT COUNT(*) FROM t_user", new ScalarHandler<>());
            return c == null ? 0 : c.intValue();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
