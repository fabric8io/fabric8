/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.configadmin;

import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class FabricConfigAdminBridge implements Runnable {

    public static final String FABRIC_ZOOKEEPER_PID = "fabric.zookeeper.pid";
    public static final String AGENT_PID = "org.fusesource.fabric.agent";
    public static final String LAST_MODIFIED = "lastModified";

    private static final Logger LOGGER = LoggerFactory.getLogger(FabricConfigAdminBridge.class);

    private ConfigurationAdmin configAdmin;
    private FabricService fabricService;

    public ConfigurationAdmin getConfigAdmin() {
        return configAdmin;
    }

    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public void init() {
        run();
    }

    public void destroy() {
        this.fabricService.unTrackConfiguration(this);
    }

    @Override
    public void run() {
        this.fabricService.trackConfiguration(this);
        try {
            update();
        } catch (Exception ex) {
          //do not propagate exception back.
        }
    }

    protected void update() {
        try {
            Profile profile = fabricService.getCurrentContainer().getOverlayProfile();
            final Map<String, Map<String, String>> pidProperties = profile.getConfigurations();
            List<Configuration> configs = asList(getConfigAdmin().listConfigurations("(" + FABRIC_ZOOKEEPER_PID + "=*)"));
            for (String pid : pidProperties.keySet()) {
                Hashtable<String, String> c = new Hashtable<String, String>();
                c.putAll(pidProperties.get(pid));
                String p[] = parsePid(pid);
                //Get the configuration by fabric zookeeper pid, pid and factory pid.
                Configuration config = getConfiguration(pid, p[0], p[1]);
                configs.remove(config);
                Dictionary props = config.getProperties();
                Hashtable old = props != null ? new Hashtable() : null;
                if (pid.equals(AGENT_PID)) {
                    c.put(LAST_MODIFIED, String.valueOf(profile.getLastModified()));
                }
                if (old != null) {
                    for (Enumeration e = props.keys(); e.hasMoreElements(); ) {
                        Object key = e.nextElement();
                        Object val = props.get(key);
                        old.put(key, val);
                    }
                    old.remove(FABRIC_ZOOKEEPER_PID);
                    old.remove(org.osgi.framework.Constants.SERVICE_PID);
                    old.remove(ConfigurationAdmin.SERVICE_FACTORYPID);
                }
                if (!c.equals(old)) {
                    LOGGER.info("Updating configuration {}", config.getPid());
                    c.put(FABRIC_ZOOKEEPER_PID, pid);
                    if (config.getBundleLocation() != null) {
                        config.setBundleLocation(null);
                    }
                    config.update(c);
                } else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Ignoring configuration {} (no changes)", config.getPid());
                    }
                }
            }
            for (Configuration config : configs) {
                LOGGER.info("Deleting configuration {}", config.getPid());
                fabricService.getPortService().unRegisterPort(fabricService.getCurrentContainer(), config.getPid());
                config.delete();
            }
        } catch (Exception e) {
            LOGGER.warn("Exception when tracking configurations. This exception will be ignored.", e);
        }
    }

    <T> List<T> asList(T... a) {
        List<T> l = new ArrayList<T>();
        if (a != null) {
            Collections.addAll(l, a);
        }
        return l;
    }

    /**
     * Splits a pid into service and factory pid.
     *
     * @param pid The pid to parse.
     * @return An arrays which contains the pid[0] the pid and pid[1] the factory pid if applicable.
     */
    String[] parsePid(String pid) {
        int n = pid.indexOf('-');
        if (n > 0) {
            String factoryPid = pid.substring(n + 1);
            pid = pid.substring(0, n);
            return new String[]{pid, factoryPid};
        } else {
            return new String[]{pid, null};
        }
    }

    Configuration getConfiguration(String zooKeeperPid, String pid, String factoryPid) throws Exception {
        String filter = "(" + FABRIC_ZOOKEEPER_PID + "=" + zooKeeperPid + ")";
        Configuration[] oldConfiguration = getConfigAdmin().listConfigurations(filter);
        if (oldConfiguration != null && oldConfiguration.length > 0) {
            return oldConfiguration[0];
        } else {
            Configuration newConfiguration;
            if (factoryPid != null) {
                newConfiguration = getConfigAdmin().createFactoryConfiguration(pid, null);
            } else {
                newConfiguration = getConfigAdmin().getConfiguration(pid, null);
            }
            return newConfiguration;
        }
    }
}
