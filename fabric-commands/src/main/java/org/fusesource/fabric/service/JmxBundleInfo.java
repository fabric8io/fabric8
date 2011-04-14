package org.fusesource.fabric.service;

import org.fusesource.fabric.api.data.BundleInfo;
import javax.management.openmbean.CompositeData;
import static org.osgi.jmx.framework.BundleStateMBean.*;


public class JmxBundleInfo extends JmxInfo implements BundleInfo {

    public JmxBundleInfo(CompositeData data) {
        super(data, IDENTIFIER);
    }

    public State getState() {
        return State.valueOf((String) data.get(STATE));
    }

    public String getSymbolicName() {
        return (String) data.get(SYMBOLIC_NAME);
    }

    public String getVersion() {
        return (String) data.get(VERSION);
    }

    public String[] getImportPackages() {
        return (String[]) data.get(IMPORTED_PACKAGES);
    }

    public String[] getExportPackages() {
        return (String[]) data.get(EXPORTED_PACKAGES);
    }
}
