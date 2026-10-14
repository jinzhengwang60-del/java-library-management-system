package com.booksys.listener;

import com.booksys.entity.User;

import javax.servlet.ServletContext;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 在线用户监听器
 * 任务书要求：在线用户列表
 *
 * 通过监听 HttpSession 的属性变化来维护在线用户集合
 */
@WebListener
public class OnlineUserListener implements HttpSessionListener, HttpSessionAttributeListener {

    /** ServletContext中存储在线用户列表使用的 key */
    public static final String ONLINE_USERS_KEY = "onlineUsers";

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        ServletContext sc = se.getSession().getServletContext();
        if (sc.getAttribute(ONLINE_USERS_KEY) == null) {
            sc.setAttribute(ONLINE_USERS_KEY, new CopyOnWriteArrayList<User>());
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        Object obj = se.getSession().getAttribute("loginUser");
        if (obj instanceof User) {
            removeOnlineUser(se.getSession().getServletContext(), (User) obj);
        }
    }

    @Override
    public void attributeAdded(HttpSessionBindingEvent event) {
        if ("loginUser".equals(event.getName()) && event.getValue() instanceof User) {
            addOnlineUser(event.getSession().getServletContext(), (User) event.getValue());
        }
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent event) {
        if ("loginUser".equals(event.getName()) && event.getValue() instanceof User) {
            removeOnlineUser(event.getSession().getServletContext(), (User) event.getValue());
        }
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent event) {
        if ("loginUser".equals(event.getName())) {
            if (event.getValue() instanceof User) {
                removeOnlineUser(event.getSession().getServletContext(), (User) event.getValue());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized void addOnlineUser(ServletContext sc, User user) {
        List<User> list = (List<User>) sc.getAttribute(ONLINE_USERS_KEY);
        if (list == null) {
            list = new CopyOnWriteArrayList<>();
            sc.setAttribute(ONLINE_USERS_KEY, list);
        }
        // 去重
        boolean exists = false;
        for (User u : list) {
            if (u.getId() != null && u.getId().equals(user.getId())) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            list.add(user);
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized void removeOnlineUser(ServletContext sc, User user) {
        List<User> list = (List<User>) sc.getAttribute(ONLINE_USERS_KEY);
        if (list != null && user.getId() != null) {
            list.removeIf(u -> user.getId().equals(u.getId()));
        }
    }
}
