package org.fusesource.fabric.boot.commands.service;

import java.util.List;

/**
 *
 */

public interface Create {
    Object run() throws Exception;

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

    public String getProfile();

    public void setProfile(String profile);

    public boolean isNonManaged();

    public void setNonManaged(boolean nonManaged);
}
