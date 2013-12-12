package io.fabric8.commands.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigWithKeyCompleter implements Completer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigWithKeyCompleter.class);

    private final StringsCompleter delegate = new StringsCompleter();

    private ConfigurationAdmin admin;

    public void setAdmin(ConfigurationAdmin admin) {
        this.admin = admin;
    }

    public void init() {
        Configuration[] configs;
        try {
            configs = admin.listConfigurations(null);
            if (configs == null) {
                return;
            }
        } catch (Exception e) {
            return;
        }

        Collection<String> pids = new ArrayList<String>();
        for (Configuration config : configs) {
            delegate.getStrings().addAll(getPidWithKeys(config.getPid()));
        }


    }

    public int complete(final String buffer, final int cursor, final List candidates) {
        int firstPass = delegate.complete(buffer, cursor, candidates);
        if (firstPass < 0) {
            updateAllPids();
            return delegate.complete(buffer, cursor, candidates);
        } else {
            return firstPass;
        }
    }

    /**
     * Updates all Pids.
     */
    private void updateAllPids() {
        Configuration[] configurations = null;
        try {
            configurations = admin.listConfigurations(null);
            if (configurations != null) {
                for (Configuration configuration:configurations) {
                      delegate.getStrings().addAll(getPidWithKeys(configuration.getPid()));
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Could not lookup pids from configuration admin.");
        }
    }

    /**
     * Returns a Set of Stings that contains all keys of the pid prefixed with the pid itself.
     * @param pid
     * @return
     */
    private Set<String> getPidWithKeys(String pid) {
        Set<String> pidWithKeys = new LinkedHashSet<String>();
        try {
            Configuration[] configuration = admin.listConfigurations("(service.pid=" + pid + ")");
            if (configuration != null && configuration.length > 0) {
                Dictionary dictionary = configuration[0].getProperties();
                if (dictionary != null) {
                    Enumeration keyEnumeration = dictionary.keys();
                    while (keyEnumeration.hasMoreElements()) {
                        String key = (String) keyEnumeration.nextElement();
                        pidWithKeys.add(pid+"/"+key);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not lookup pid {} from configuration admin.",pid);
        } catch (InvalidSyntaxException e) {
            LOGGER.warn("Could not lookup pid {} from configuration admin.",pid);
        }
        return pidWithKeys;
    }
}