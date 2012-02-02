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

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.linkedin.zookeeper.tracker.*;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.StringReader;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ZooKeeperConfigAdminBridge implements NodeEventsListener<String>, LifecycleListener {

    public static final String DELETED = "#deleted#";

    public static final String FABRIC_ZOOKEEPER_PID = "fabric.zookeeper.pid";

    public static final String FILEINSTALL = "felix.fileinstall.filename";

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
            version = zooKeeper.getStringData(ZkPath.CONFIG_CONTAINER.getPath(name));
            if (version == null) {
                throw new IllegalStateException("Configuration for node " + name + " not found at " + ZkPath.CONFIG_CONTAINER.getPath(name));
            }
            node = ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, name);
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
                    data = data.trim();
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

    static public Properties toProperties(String source) throws IOException {
        Properties rc = new Properties();
        rc.load(new StringReader(source));
        return rc;
    }

    static public String stripSuffix(String value, String suffix) {
        if(value.endsWith(suffix)) {
            return value.substring(0, value.length() -suffix.length());
        } else {
            return value;
        }
    }

    public Dictionary load(String pid) throws IOException {
        try {
            Hashtable props = new Hashtable();
            load(pid, node, props);
            InterpolationHelper.performSubstitution(props, new InterpolationHelper.SubstitutionCallback() {
                public String getValue(String key) {
                    if (key.startsWith("zk:")) {
                        try {
                            return new String(ZkPath.loadURL(zooKeeper, key), "UTF-8");
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Could not load zk value: "+key, e);
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
        TrackedNode<String> cfg = tree != null ? tree.getTree().get(node + "/" + pid+".properties") : null;
        if (cfg != null) {
        //if (cfg != null && !DELETED.equals(cfg.getData())) {
            Properties properties = toProperties(cfg.getData());

            // clear out the dict if it had a deleted key.
            if( properties.remove(DELETED)!=null ) {
                Enumeration keys = dict.keys();
                while (keys.hasMoreElements()) {
                    dict.remove(keys.nextElement());
                }
            }

            for (Map.Entry<Object, Object> entry: properties.entrySet()){
                if( DELETED.equals(entry.getValue()) ) {
                    dict.remove(entry.getKey());
                } else {
                    dict.put(entry.getKey(), entry.getValue());
                }
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
            if(pid.endsWith(".properties")) {
                pid = stripSuffix(pid, ".properties");
                pids.add(pid);
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
                    boolean changed = false;
                    if (props != null) {
                        for (Enumeration e = c.keys(); e.hasMoreElements();) {
                            Object key = e.nextElement();
                            Object val = c.get(key);
                            Object oldVal = props.get(key);
                            if (oldVal == null || !oldVal.equals(val)) {
                                props.put(key, val);
                                LOGGER.info(config.getPid() + " - property '" + key + "' changed: " + oldVal + " -> " + val);
                                changed = true;
                            }
                        }
                        props.remove(FABRIC_ZOOKEEPER_PID);
                        props.remove(org.osgi.framework.Constants.SERVICE_PID);
                        props.remove(ConfigurationAdmin.SERVICE_FACTORYPID);

                        if (changed) {
                            LOGGER.info(config.getPid() + " - updating configuration");
                            config.update(props);
                            saveProperties(props);
                        } else {
                            LOGGER.fine(config.getPid() + " - ignoring configuration (no changes)");
                        }
                    } else {
                        LOGGER.info(config.getPid() + " - initializing configuration");
                        config.update(c);
                    }
                }
                for (Configuration config : configs) {
                    LOGGER.info(config.getPid() + " - deleting configuration");
                    config.delete();
                }
            }
            LOGGER.exiting(getClass().getName(), "onEvents");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Exception when tracking configurations", e);
        }
    }

    private void saveProperties(Dictionary props) throws IOException {
        String fileInstall = (String) props.get(FILEINSTALL);
        if (fileInstall != null) {
            URL configUrl = new URL(fileInstall);
            props.remove(FILEINSTALL);
            Properties fileProperties = createProperties(props);
            fileProperties.store(new FileOutputStream(configUrl.getFile()), "Saved by " + name + " at  " + new Date());
        }
    }

    private Properties createProperties(Dictionary dict) {
        Properties p = new Properties();
        Enumeration<String> keys = dict.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            p.setProperty(key, (String)dict.get(key));
        }
        return p;
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
