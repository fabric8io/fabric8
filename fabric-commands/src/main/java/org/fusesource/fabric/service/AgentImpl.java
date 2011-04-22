/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.data.BundleInfo;
import org.fusesource.fabric.api.data.ServiceInfo;
import org.fusesource.fabric.service.JmxTemplate.BundleStateCallback;
import org.fusesource.fabric.service.JmxTemplate.ServiceStateCallback;
import org.linkedin.zookeeper.client.IZKClient;
import org.osgi.jmx.framework.BundleStateMBean;
import org.osgi.jmx.framework.ServiceStateMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentImpl implements Agent {

    /**
     * Logger.
     */
    private Logger logger = LoggerFactory.getLogger(AgentImpl.class);

    private final Agent parent;
    private final String id;
    private final IZKClient zooKeeper;

    public AgentImpl(Agent parent, String id, IZKClient zooKeeper) {
        this.parent = parent;
        this.id = id;
        this.zooKeeper = zooKeeper;
    }

    public Agent getParent() {
        return parent;
    }

    public String getId() {
        return id;
    }

    public boolean isAlive() {
        try {
            return zooKeeper.exists("/fabric/registry/agents/alive/" + id) != null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isRoot() {
        return parent == null;
    }

    public String getSshUrl() {
        return getZkData("ssh");
    }

    public String getJmxUrl() {
        return getZkData("jmx");
    }

    private String getZkData(String name) {
        try {
            return zooKeeper.getStringData("/fabric/registry/agents/config/" + id + "/" + name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public BundleInfo[] getBundles() {
        try {
            return new JmxTemplate().execute(this, new BundleStateCallback<BundleInfo[]>() {
                public BundleInfo[] doWithBundleState(BundleStateMBean bundleState) throws Exception {
                    TabularData bundles = bundleState.listBundles();
                    BundleInfo[] info = new BundleInfo[bundles.size()];

                    int i = 0;
                    for (Object data : bundles.values().toArray()) {
                        info[i++] = new JmxBundleInfo((CompositeData) data);
                    }

                    // sort bundles using bundle id to preserve same order like in framework
                    Arrays.sort(info, new BundleInfoComparator());
                    return info;
                }
            });
        } catch (Exception e) {
            logger.error("Error while retrieving bundles", e);
            return new BundleInfo[0];
        }
    }

    public ServiceInfo[] getServices() {
        try {
            return new JmxTemplate().execute(this, new ServiceStateCallback<ServiceInfo[]>() {
                public ServiceInfo[] doWithServiceState(ServiceStateMBean serviceState) throws Exception {
                    TabularData services = serviceState.listServices();
                    ServiceInfo[] info = new ServiceInfo[services.size()];

                    int i = 0;
                    for (Object data : services.values().toArray()) {
                        info[i++] = new JmxServiceInfo((CompositeData) data);
                    }

                    // sort services using service id to preserve same order like in framework
                    Arrays.sort(info, new ServiceInfoComparator());
                    return info;
                }
            });
        } catch (Exception e) {
            logger.error("Error while retrieving services", e);
            return new ServiceInfo[0];
        }
    }

    public Profile[] getProfiles() {
        try {
            String version = zooKeeper.getStringData("/fabric/configs/agents/" + id);
            String node = "/fabric/configs/versions/" + version + "/agents/" + id;
            String str = zooKeeper.getStringData(node);
            if (str == null) {
                return new Profile[0];
            }
            List<Profile> profiles = new ArrayList<Profile>();
            for (String p : str.split(" ")) {
                profiles.add(new ProfileImpl(p, version, zooKeeper));
            }
            return profiles.toArray(new Profile[profiles.size()]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setProfiles(Profile[] profiles) {
        try {
            String version = zooKeeper.getStringData("/fabric/configs/agents/" + id);
            String node = "/fabric/configs/versions/" + version + "/agents/" + id;
            String str = "";
            for (Profile parent : profiles) {
                if (!version.equals(parent.getVersion())) {
                    throw new IllegalArgumentException("Bad profile: " + parent);
                }
                if (!str.isEmpty()) {
                    str += " ";
                }
                str += parent.getId();
            }
            zooKeeper.setData( node, str );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
