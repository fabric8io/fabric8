/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.internal;

import java.util.ArrayList;
import java.util.List;

import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.service.FabricServiceImpl;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentImpl implements Agent {

    /**
     * Logger.
     */
    protected transient Logger logger = LoggerFactory.getLogger(getClass());

    private final Agent parent;
    private final String id;
    private final FabricServiceImpl service;

    public AgentImpl(Agent parent, String id, FabricServiceImpl service) {
        this.parent = parent;
        this.id = id;
        this.service = service;
    }

    public Agent getParent() {
        return parent;
    }

    public String getId() {
        return id;
    }

    public boolean isAlive() {
        try {
            return service.getZooKeeper().exists(ZkPath.AGENT_ALIVE.getPath(id)) != null;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public boolean isRoot() {
        return parent == null;
    }

    public String getSshUrl() {
        return getZkData(ZkPath.AGENT_SSH);
    }

    public String getJmxUrl() {
        return getZkData(ZkPath.AGENT_JMX);
    }

    private String getZkData(ZkPath path) {
        try {
            return service.getZooKeeper().getStringData(path.getPath(id));
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public Version getVersion() {
        try {
            String version = service.getZooKeeper().getStringData(ZkPath.CONFIG_AGENT.getPath(id));
            return new VersionImpl(version, service);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setVersion(Version version) {
        try {
            ZooKeeperUtils.set( service.getZooKeeper(), ZkPath.CONFIG_AGENT.getPath(id), version.getName() );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Profile[] getProfiles() {
        try {
            String version = service.getZooKeeper().getStringData(ZkPath.CONFIG_AGENT.getPath(id));
            String node = ZkPath.CONFIG_VERSIONS_AGENT.getPath(version, id);
            String str = service.getZooKeeper().getStringData(node);
            if (str == null) {
                return new Profile[0];
            }
            List<Profile> profiles = new ArrayList<Profile>();
            for (String p : str.split(" ")) {
                profiles.add(new ProfileImpl(p, version, service));
            }
            return profiles.toArray(new Profile[profiles.size()]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setProfiles(Profile[] profiles) {
        try {
            String version = service.getZooKeeper().getStringData(ZkPath.CONFIG_AGENT.getPath(id));
            String node = ZkPath.CONFIG_VERSIONS_AGENT.getPath(version, id);
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
            service.getZooKeeper().setData(node, str);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public String getLocation() {
        try {
            String path = ZkPath.AGENT_LOCATION.getPath(id);
            if (service.getZooKeeper().exists(path) != null) {
                return service.getZooKeeper().getStringData(path);
            } else {
                return "";
            }
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public void setLocation(String location) {
        try {
            String path = ZkPath.AGENT_LOCATION.getPath(id);
            ZooKeeperUtils.set( service.getZooKeeper(), path, location );
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public void start() {
        service.startAgent(this);
    }

    @Override
    public void stop() {
        service.stopAgent(this);
    }

    @Override
    public void destroy() {
        service.destroy(this);
    }

    public Agent[] getChildren() {
        return AgentFactory.createChildren(this, service);
    }

    public String getType() {
        return "karaf";
    }
}
