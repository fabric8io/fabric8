/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.configadmin;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.linkedin.zookeeper.tracker.NodeEvent;
import org.linkedin.zookeeper.tracker.NodeEventsListener;
import org.linkedin.zookeeper.tracker.TrackedNode;
import org.linkedin.zookeeper.tracker.ZKStringDataReader;
import org.linkedin.zookeeper.tracker.ZooKeeperTreeTracker;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ZooKeeperConfigAdminBridge implements NodeEventsListener<String>, LifecycleListener {

    public static final String DELETED = "#deleted#";

    public static final String FABRIC_ZOOKEEPER_PID = "fabric.zookeeper.pid";

    private static final Logger LOGGER = Logger.getLogger(ZooKeeperConfigAdminBridge.class.getName());

    private IZKClient zooKeeper;
    private ConfigurationAdmin configAdmin;
    private String name;
    private String version;
    private String node;
    private Map<String, ZooKeeperTreeTracker<String>> trees = new ConcurrentHashMap<String, ZooKeeperTreeTracker<String>>();
    private boolean tracking = false;


    public void init() throws Exception {
    }

    public void destroy() throws Exception {
        for (ZooKeeperTreeTracker<String> tree : trees.values()) {
            tree.destroy();
        }
        trees.clear();
    }

    public void onConnected() {
        try {
            // Find our root node
            version = zooKeeper.getStringData(ZkPath.CONFIG_AGENT.getPath(name));
            if (version == null) {
                throw new IllegalStateException("Configuration for node " + name + " not found at " + ZkPath.CONFIG_AGENT.getPath(name));
            }
            node = ZkPath.CONFIG_VERSIONS_AGENT.getPath(version, name);
            if (zooKeeper.exists(node) == null) {
                zooKeeper.createWithParents(node, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            trees = new ConcurrentHashMap<String, ZooKeeperTreeTracker<String>>();
            tracking = true;
            try {
                track(node);
            } finally {
                tracking = false;
            }
            onEvents(null);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Exception when tracking configurations", e);
        }
    }

    public void onDisconnected() {
    }

    protected ZooKeeperTreeTracker<String> track(String path) throws InterruptedException, KeeperException {
        ZooKeeperTreeTracker<String> tree = trees.get(path);
        if (tree == null) {
            if (zooKeeper.exists(path) != null) {
                tree = new ZooKeeperTreeTracker<String>(zooKeeper, new ZKStringDataReader(), path);
                trees.put(path, tree);
                tree.track(this);
                String data = tree.getTree().get(path).getData();
                if (data != null) {
                    String[] parents = data.split(" ");
                    for (String parent : parents) {
                        track(ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, parent));
                    }
                }
            } else {
                // If the node does not exist yet, we track the parent to make
                // sure we receive the node creation event
                String p = ZkPath.CONFIG_VERSIONS_PROFILES.getPath(version);
                if (!trees.containsKey(p)) {
                    tree = new ZooKeeperTreeTracker<String>(zooKeeper, new ZKStringDataReader(), p, 1);
                    trees.put(p, tree);
                    tree.track(this);
                }
                return null;
            }
        }

        return tree;
    }


    public Dictionary load(String pid) throws IOException {
        try {
            Hashtable props = new Hashtable();
            load(pid, node, props);
            InterpolationHelper.performSubstitution(props, new InterpolationHelper.SubstitutionCallback() {
                public String getValue(String key) {
                    if (key.startsWith("zk:")) {
                        key = key.substring("zk:".length());
                        if (key.charAt(0) != '/') {
                            key = ZkPath.AGENT.getPath(key);
                        }
                        try {
                            return zooKeeper.getStringData(key);
                        } catch (Exception e) {
                        }
                    }
                    return null;
                }
            });
            return props;
        } catch (InterruptedException e) {
            throw (IOException) new InterruptedIOException("Error loading pid " + pid).initCause(e);
        } catch (KeeperException e) {
            throw (IOException) new IOException("Error loading pid " + pid).initCause(e);
        }
    }

    private void load(String pid, String node, Dictionary dict) throws KeeperException, InterruptedException, IOException {
        ZooKeeperTreeTracker<String> tree = track(node);
        TrackedNode<String> root = tree != null ? tree.getTree().get(node) : null;
        String[] parents = root != null && root.getData() != null ? root.getData().split(" ") : new String[0];
        for (String parent : parents) {
            load(pid, ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, parent), dict);
        }
        TrackedNode<String> cfg = tree != null ? tree.getTree().get(node + "/" + pid) : null;
        if (cfg != null && !DELETED.equals(cfg.getData())) {
            for (String key : getChildren(tree, node + "/" + pid)) {
                TrackedNode<String> n = tree.getTree().get(node + "/" + pid + "/" + key);
                dict.put(key, n.getData() != null ? n.getData() : "");
            }
        }
    }

    private Set<String> getPids() throws KeeperException, InterruptedException {
        Set<String> pids = new HashSet<String>();
        getPids(node, pids);
        return pids;
    }

    private void getPids(String node, Set<String> pids) throws KeeperException, InterruptedException {
        ZooKeeperTreeTracker<String> tree = track(node);
        TrackedNode<String> root = tree != null ? tree.getTree().get(node) : null;
        String[] parents = root != null && root.getData() != null ? root.getData().split(" ") : new String[0];
        for (String parent : parents) {
            getPids(ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, parent), pids);
        }
        for (String pid : getChildren(tree, node)) {
            TrackedNode<String> n = tree.getTree().get(node + "/" + pid);
            if (n != null) {
                if (DELETED.equals(n.getData())) {
                    pids.remove(pid);
                } else {
                    pids.add(pid);
                }
            }
        }
    }

    protected List<String> getChildren(ZooKeeperTreeTracker<String> tree, String node) {
        List<String> children = new ArrayList<String>();
        if (tree != null) {
            Pattern p = Pattern.compile(node + "/[^/]*");
            for (String c : tree.getTree().keySet()) {
                if (p.matcher(c).matches()) {
                    children.add(c.substring(c.lastIndexOf('/') + 1));
                }
            }
        }
        return children;
    }

    public void onEvents(Collection<NodeEvent<String>> nodeEvents) {
        LOGGER.entering(getClass().getName(), "onEvents", nodeEvents);
        try {
            if (!tracking) {
                final Set<String> pids = getPids();
                List<Configuration> configs = asList(getConfigAdmin().listConfigurations("(" + FABRIC_ZOOKEEPER_PID + "=*)"));
                for (String pid : pids) {
                    Dictionary c = load(pid);
                    String p[] = parsePid(pid);
                    Configuration config = getConfiguration(pid, p[0], p[1]);
                    configs.remove(config);
                    Dictionary props = config.getProperties();
                    Hashtable old = props != null ? new Hashtable() : null;
                    if (old != null) {
                        for (Enumeration e = props.keys(); e.hasMoreElements();) {
                            Object key = e.nextElement();
                            Object val = props.get(key);
                            old.put(key, val);
                        }
                        old.remove(FABRIC_ZOOKEEPER_PID);
                        old.remove(org.osgi.framework.Constants.SERVICE_PID);
                        old.remove(ConfigurationAdmin.SERVICE_FACTORYPID);
                    }
                    if (!c.equals(old)) {
                        LOGGER.info("Updating configuration " + config.getPid());
                        c.put(FABRIC_ZOOKEEPER_PID, pid);
                        if (config.getBundleLocation() != null) {
                            config.setBundleLocation(null);
                        }
                        config.update(c);
                    } else {
                        LOGGER.info("Ignoring configuration " + config.getPid() + " (no changes)");
                    }
                }
                for (Configuration config : configs) {
                    LOGGER.info("Deleting configuration " + config.getPid());
                    config.delete();
                }
            }
            LOGGER.exiting(getClass().getName(), "onEvents");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Exception when tracking configurations", e);
        }
    }

    public static <T> List<T> asList(T... a) {
        List<T> l = new ArrayList<T>();
        if (a != null) {
            Collections.addAll(l, a);
        }
        return l;
    }

    String[] parsePid(String pid) {
        int n = pid.indexOf('-');
        if (n > 0) {
            String factoryPid = pid.substring(n + 1);
            pid = pid.substring(0, n);
            return new String[]
                    {
                            pid, factoryPid
                    };
        } else {
            return new String[]
                    {
                            pid, null
                    };
        }
    }

    Configuration getConfiguration(String zooKeeperPid, String pid, String factoryPid)
            throws Exception {
        Configuration oldConfiguration = findExistingConfiguration(zooKeeperPid);
        if (oldConfiguration != null) {
            return oldConfiguration;
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

    Configuration findExistingConfiguration(String zooKeeperPid) throws Exception {
        String filter = "(" + FABRIC_ZOOKEEPER_PID + "=" + zooKeeperPid + ")";
        Configuration[] configurations = getConfigAdmin().listConfigurations(filter);
        if (configurations != null && configurations.length > 0) {
            return configurations[0];
        } else {
            return null;
        }
    }

    public IZKClient getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(IZKClient zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    public ConfigurationAdmin getConfigAdmin() {
        return configAdmin;
    }

    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
