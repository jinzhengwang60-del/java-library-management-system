package com.booksys.listener;
import com.booksys.util.DbUtil;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class DatabaseLifecycleListener implements ServletContextListener {
    @Override
    public void contextDestroyed(ServletContextEvent event) { DbUtil.close(); }
}

