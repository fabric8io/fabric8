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
package org.fusesource.fabric.internal;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.ZKData;

public class ZooKeeperUtils {

    public static void copy( IZKClient source, IZKClient dest, String path ) throws InterruptedException, KeeperException {
        for (String child : source.getChildren(path)) {
            child = path + "/" + child;
            if (dest.exists(child) == null) {
                byte[] data  = source.getData(child);
                dest.createBytesNodeWithParents(child, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                copy(source, dest, child);
            }
        }
    }

    public static void copy( IZKClient zk, String from, String to ) throws InterruptedException, KeeperException {
        for (String child : zk.getChildren(from)) {
            String fromChild = from + "/" + child;
            String toChild = to + "/" + child;
            if (zk.exists(toChild) == null) {
                byte[] data  = zk.getData(fromChild);
                zk.createBytesNodeWithParents(toChild, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                copy(zk, fromChild, toChild);
            }
        }
    }

    public static void add( IZKClient zooKeeper, String path, String value ) throws InterruptedException, KeeperException {
        if (zooKeeper.exists(path) == null) {
            zooKeeper.createOrSetWithParents(path, value, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } else {
            String data = zooKeeper.getStringData( path );
            if (data == null) {
                data = "";
            }
            if (data.length() > 0) {
                data += " ";
            }
            data += value;
            zooKeeper.setData( path, data );
        }
    }

    public static void remove( IZKClient zooKeeper, String path, String value ) throws InterruptedException, KeeperException {
        if (zooKeeper.exists(path) != null) {
            String data = zooKeeper.getStringData( path );
            List<String> parts = new ArrayList<String>(Arrays.asList(data.split(" ")));
            boolean changed = false;
            for (Iterator<String> it = parts.iterator(); it.hasNext();) {
                String v = it.next();
                if (v.matches(value)) {
                    it.remove();
                    changed = true;
                }
            }
            if (changed) {
                data = "";
                for (String part : parts) {
                    if (data.length() > 0) {
                        data += " ";
                    }
                    data += part;
                }
                zooKeeper.setData( path, data );
            }
        }
    }

    public static String get( IZKClient zooKeeper, String path ) throws InterruptedException, KeeperException {
        return zooKeeper.getStringData( path );
    }

    public static void set( IZKClient zooKeeper, String path, String value ) throws InterruptedException, KeeperException {
        zooKeeper.createOrSetWithParents(path, value, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }

    public static void create( IZKClient zooKeeper, String path ) throws InterruptedException, KeeperException {
        zooKeeper.createWithParents(path, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }

    public static void createDefault( IZKClient zooKeeper, String path, String value ) throws InterruptedException, KeeperException {
        if (zooKeeper.exists(path) == null) {
            zooKeeper.createWithParents( path, value, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT );
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

}
