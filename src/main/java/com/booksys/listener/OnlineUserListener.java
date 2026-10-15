package com.booksys.listener;

import com.booksys.entity.User;
import javax.servlet.ServletContext;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@WebListener
public class OnlineUserListener implements HttpSessionListener, HttpSessionAttributeListener, HttpSessionIdListener {
    public static final String ONLINE_USERS_KEY = "onlineUsers";
    private static final String SESSIONS_KEY = OnlineUserListener.class.getName() + ".sessions";

    @SuppressWarnings("unchecked")
    private static Map<String, User> sessions(ServletContext context) {
        synchronized (context) {
            Map<String, User> sessions = (Map<String, User>) context.getAttribute(SESSIONS_KEY);
            if (sessions == null) {
                sessions = new ConcurrentHashMap<>();
                context.setAttribute(SESSIONS_KEY, sessions);
            }
            return sessions;
        }
    }

    private static void publish(ServletContext context) {
        synchronized (context) {
            Map<Integer, User> users = new TreeMap<>();
            for (User current : sessions(context).values()) {
                User summary = new User();
                summary.setId(current.getId());
                summary.setUsername(current.getUsername());
                summary.setRole(current.getRole());
                users.put(summary.getId(), summary);
            }
            context.setAttribute(ONLINE_USERS_KEY, new ArrayList<>(users.values()));
        }
    }

    private static void update(HttpSession session) {
        ServletContext context = session.getServletContext();
        Object current = session.getAttribute("loginUser");
        if (current instanceof User) sessions(context).put(session.getId(), (User) current);
        else sessions(context).remove(session.getId());
        publish(context);
    }

    @Override public void sessionCreated(HttpSessionEvent event) { publish(event.getSession().getServletContext()); }
    @Override public void sessionDestroyed(HttpSessionEvent event) {
        ServletContext context = event.getSession().getServletContext();
        sessions(context).remove(event.getSession().getId());
        publish(context);
    }
    @Override public void attributeAdded(HttpSessionBindingEvent event) {
        if ("loginUser".equals(event.getName())) update(event.getSession());
    }
    @Override public void attributeRemoved(HttpSessionBindingEvent event) {
        if ("loginUser".equals(event.getName())) {
            ServletContext context = event.getSession().getServletContext();
            sessions(context).remove(event.getSession().getId());
            publish(context);
        }
    }
    @Override public void attributeReplaced(HttpSessionBindingEvent event) {
        if ("loginUser".equals(event.getName())) update(event.getSession());
    }
    @Override public void sessionIdChanged(HttpSessionEvent event, String oldId) {
        ServletContext context = event.getSession().getServletContext();
        sessions(context).remove(oldId);
        update(event.getSession());
    }
}
