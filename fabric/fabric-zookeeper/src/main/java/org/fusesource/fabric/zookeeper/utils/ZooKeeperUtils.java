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
package org.fusesource.fabric.zookeeper.utils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.linkedin.zookeeper.client.ZKData;

public final class ZooKeeperUtils {

    private ZooKeeperUtils() {
        //Utility Class
    }

    public static void copy(IZKClient source, IZKClient dest, String path) throws InterruptedException, KeeperException {
        for (String child : source.getChildren(path)) {
            child = path + "/" + child;
            if (dest.exists(child) == null) {
                byte[] data  = source.getData(child);
                set(dest, child, data);
                copy(source, dest, child);
            }
        }
    }

    public static void copy(IZKClient zk, String from, String to) throws InterruptedException, KeeperException {
        for (String child : zk.getChildren(from)) {
            String fromChild = from + "/" + child;
            String toChild = to + "/" + child;
            if (zk.exists(toChild) == null) {
                byte[] data  = zk.getData(fromChild);
                set(zk, toChild, data);
                copy(zk, fromChild, toChild);
            }
        }
    }

    public static void add(IZKClient zooKeeper, String path, String value) throws InterruptedException, KeeperException {
        if (zooKeeper.exists(path) == null) {
            zooKeeper.createOrSetWithParents(path, value, CreateMode.PERSISTENT);
        } else {
            String data = zooKeeper.getStringData(path);
            if (data == null) {
                data = "";
            }
            if (data.length() > 0) {
                data += " ";
            }
            data += value;
            zooKeeper.setData(path, data);
        }
    }

    public static void remove(IZKClient zooKeeper, String path, String value ) throws InterruptedException, KeeperException {
        if (zooKeeper.exists(path) != null) {
            List<String> parts = new LinkedList<String>();
            String data = zooKeeper.getStringData( path );
            if (data != null) {
                parts = new ArrayList<String>(Arrays.asList(data.split(" ")));
            }
            boolean changed = false;
            StringBuilder sb = new StringBuilder();
            for (Iterator<String> it = parts.iterator(); it.hasNext();) {
                String v = it.next();
                if (v.matches(value)) {
                    it.remove();
                    changed = true;
                }
            }
            if (changed) {
                sb.delete(0, sb.length());
                for (String part : parts) {
                    if (data.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append(part);
                }
                zooKeeper.setData(path, sb.toString());
            }
        }
    }

    public static String get(IZKClient zooKeeper, String path) throws InterruptedException, KeeperException {
        return zooKeeper.getStringData( path);
    }

    public static void set(IZKClient zooKeeper, String path, String value) throws InterruptedException, KeeperException {
        zooKeeper.createOrSetWithParents(path, value, CreateMode.PERSISTENT);
    }

    public static void set(IZKClient zooKeeper, String path, byte[] value) throws InterruptedException, KeeperException {
        if(zooKeeper.exists(path) != null) {
            zooKeeper.setByteData(path, value);
        }
        try {
            zooKeeper.createWithParents(path, value, CreateMode.PERSISTENT);
        } catch(KeeperException.NodeExistsException e) {
            // this should not happen very often (race condition)
            zooKeeper.setByteData(path, value);
        }
    }

    public static void create(IZKClient zooKeeper, String path) throws InterruptedException, KeeperException {
        zooKeeper.createWithParents(path, CreateMode.PERSISTENT);
    }

    public static void createDefault(IZKClient zooKeeper, String path, String value) throws InterruptedException, KeeperException {
        if (zooKeeper.exists(path) == null) {
            zooKeeper.createWithParents( path, value, CreateMode.PERSISTENT );
        }
    }

    public static Properties getProperties(IZKClient zooKeeper, String path, Watcher watcher) throws InterruptedException, KeeperException {
        ZKData<String> zkData = zooKeeper.getZKStringData(path, watcher);
        String value = zkData.getData();
        Properties properties = new Properties();
        if (value != null) {
            try {
                properties.load(new StringReader(value));
            } catch (IOException ignore) {}
        }
        return properties;
    }

    public static Properties getProperties(IZKClient zooKeeper, String path) throws InterruptedException, KeeperException {
        ZKData<String> zkData = zooKeeper.getZKStringData(path);
        String value = zkData.getData();
        Properties properties = new Properties();
        if (value != null) {
            try {
                properties.load(new StringReader(value));
            } catch (IOException ignore) {}
        }
        return properties;
    }

    public static void setProperties(IZKClient zooKeeper, String path, Properties properties) throws InterruptedException, KeeperException {
        StringWriter writer = new StringWriter();
        try {
            properties.store(writer, null);
            zooKeeper.setData(path, writer.toString());
        } catch (IOException e) {}
    }

    public static String getSubstitutedPath(final IZKClient zooKeeper, String path) throws InterruptedException, KeeperException, IOException, URISyntaxException {
        String normaledPath = path != null && path.contains("#") ? path.substring(0,path.lastIndexOf('#')) : path;
        if (normaledPath != null && zooKeeper.exists(normaledPath) != null) {
            byte[] data = ZkPath.loadURL(zooKeeper, path);
            if (data != null && data.length > 0) {
                String str = new String(ZkPath.loadURL(zooKeeper, path), "UTF-8");
                return getSubstitutedData(zooKeeper, str);
            }
        }
        return null;
    }

    public static String getSubstitutedData(final IZKClient zooKeeper, String data) throws   URISyntaxException {
        Map<String, String> props = new HashMap<String, String>();
        props.put("data", data);

        InterpolationHelper.performSubstitution(props, new InterpolationHelper.SubstitutionCallback() {
            @Override
            public String getValue(String key) {
                if (key.startsWith("zk:")) {
                    try {
                        return new String(ZkPath.loadURL(zooKeeper, key), "UTF-8");
                    } catch (Exception e) {
                        //ignore and just return null.
                    }
                }
                return null;
            }
        });
        return props.get("data");
    }

    /**
     * Generate a random String that can be used as a Zookeeper password.
     * @return
     */
    public static String generatePassword() {
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            long l = Math.round(Math.floor(Math.random() * (26 * 2 + 10)));
            if (l < 10) {
                password.append((char) ('0' + l));
            } else if (l < 36) {
                password.append((char) ('A' + l - 10));
            } else {
                password.append((char) ('a' + l - 36));
            }
        }
        return password.toString();
    }

}
