package org.fusesource.fabric.internal;

import java.util.Collection;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.fusesource.fabric.api.data.BundleInfo;
import org.osgi.jmx.JmxConstants;

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

    public Header[] getHeaders() {
        TabularData headers = (TabularData) data.get(HEADERS);
        Header[] hdr = new Header[headers.size()];
        int i = 0;
        for (CompositeData data : (Collection<CompositeData>) headers.values()) {
            String key = data.get(JmxConstants.KEY).toString();
            String value = data.get(JmxConstants.VALUE).toString();
            hdr[i++] = new HeaderImpl(key, value);
        }
        return hdr;
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

    static class HeaderImpl implements Header {
        final String key;
        final String value;

        HeaderImpl(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }
}
