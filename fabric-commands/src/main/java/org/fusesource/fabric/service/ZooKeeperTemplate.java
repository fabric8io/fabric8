/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.service;

import org.linkedin.zookeeper.client.IZKClient;

import java.util.List;

/**
 * Utility class to work with zookeeper.
 */
public class ZooKeeperTemplate {

    private IZKClient client;

    public ZooKeeperTemplate(IZKClient client) {
        this.client = client;
    }

    public String execute(String path) {
        return execute(path, new StringCallback());
    }

    public <T> T execute(String path, DataCallback<T> callback) {
        try {
            return callback.execute(client.getStringData(path));
        } catch (Exception e) {
            throw new RuntimeException("Unable to get or process data from path " + path, e);
        }
    }

    public <T> T execute(String path, ChildrenCallback<T> callback) {
        try {
            return callback.execute(client.getChildren(path));
        } catch (Exception e) {
            throw new RuntimeException("Unable to get or process children list from path " + path, e);
        }
    }

    interface DataCallback<T> {
        T execute(String data) throws Exception;
    }

    interface ChildrenCallback<T> {
        T execute(List<String> children) throws Exception;
    }

    // default implementations

    public static class ChildrenAsArrayCallback implements ChildrenCallback<String[]> {
        public String[] execute(List<String> children) throws Exception {
            return children.toArray(new String[children.size()]);
        }
    }

    public static class StringCallback implements DataCallback<String> {
        public String execute(String data) throws Exception {
            return data;
        }
    }

    public static class StringArrayCallback implements DataCallback<String[]> {
        public String[] execute(String data) throws Exception {
            if (data == null) {
                return new String[0];
            }
            return data.split("\n");
        }
    }

}
