package org.fusesource.fabric.boot.commands.service;

import java.util.List;
import java.util.Set;

/**
 *
 */

public interface Create {
    Object run() throws Exception;

    public String getBindAddress();

    public void setBindAddress(String bindAddress);

    boolean isClean();

    void setClean(boolean clean);

    boolean isNoImport();

    void setNoImport(boolean noImport);

    String getImportDir();

    void setImportDir(String importDir);

    boolean isVerbose();

    void setVerbose(boolean verbose);

    long getEnsembleStartupTime();

    void setEnsembleStartupTime(long ensembleStartupTime);

    List<String> getContainers();

    void setContainers(List<String> containers);

    public Set<String> getProfiles();

    public void setProfiles(Set<String> profile);

    public boolean isNonManaged();

    public void setNonManaged(boolean nonManaged);

    public void setResolver(String resolver);

    public String getResolver();

    public void setGlobalResolver(String resolver);

    public String getGlobalResolver();

    public void setManualIp(String manualIp);

    public String getManualIp();

    int getMinimumPort();

    void setMinimumPort(int minimumPort);

    int getMaximumPort();

    void setMaximumPort(int maximumPort);

    String getZookeeperPassword();

    void setZookeeperPassword(String zookeeperPassword);

    String getNewUser();

    void setNewUser(String newUser);

    String getNewUserPassword();

    void setNewUserPassword(String newUserPassword);

    String getNewUserRole();

    void setNewUserRole(String newUserRole);

    public boolean isGenerateZookeeperPassword();

    public void setGenerateZookeeperPassword(boolean generateZookeeperPassword);
}
