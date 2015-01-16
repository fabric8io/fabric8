package org.jboss.fuse.rhaccess.listener;


import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import io.hawt.system.ConfigManager;

public class SupportContextListener implements ServletContextListener {

    private ConfigManager configManager = new ConfigManager();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            configManager.init();
        } catch (Exception e) {
            throw createServletException(e);
        }
        sce.getServletContext().setAttribute("ConfigManager", configManager);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            configManager.destroy();
        } catch (Exception e) {
            throw createServletException(e);
        }

    }

    protected RuntimeException createServletException(Exception e) {
        return new RuntimeException(e);
    }

}
