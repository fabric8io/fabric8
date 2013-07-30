package org.fusesource.fabric.boot.commands.service;


import org.apache.curator.framework.CuratorFramework;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 *
 */

public interface Join {
    Object run() throws Exception;

    void setConfigurationAdmin(ConfigurationAdmin configurationAdmin);

    String getVersion();

    void setVersion(String version);

    String getZookeeperUrl();

    void setZookeeperUrl(String zookeeperUrl);

    String getZookeeperPassword();

    void setZookeeperPassword(String zookeeperPassword);

    public boolean isNonManaged();

    public void setNonManaged(boolean nonManaged);

    public boolean isForce();

    public void setForce(boolean force);

    public String getProfile();

    public void setProfile(String profile);

    public String getContainerName();

    public void setContainerName(String containerName);

    public void setResolver(String resolver);

    public String getResolver();

    public void setManualIp(String manualIp);

    public String getManualIp();

}
