package org.fusesource.fabric.api.data;

public interface BundleInfo {

    enum State {
        INSTALLED,
        RESOLVED,
        STARTING,
        ACTIVE,
        STOPPING,
        UNINSTALLED,
        UNKNOWN
    }

    State getState();
    String getSymbolicName();
    Long getId();
    String getVersion();

    String[] getImportPackages();
    String[] getExportPackages();

}
