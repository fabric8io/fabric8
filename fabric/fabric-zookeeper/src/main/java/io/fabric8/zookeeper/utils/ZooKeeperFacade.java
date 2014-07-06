/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.zookeeper.utils;

import io.fabric8.common.util.Strings;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.List;

/**
 * A helper class for working with ZooKeeper
 */
public class ZooKeeperFacade {
    private final CuratorFramework curator;

    public ZooKeeperFacade(CuratorFramework curator) {
        this.curator = curator;
    }

    /**
     * Returns the non-null String data for all the children matching the given path pattern
     */
    public List<String> matchingDescendantStringData(String pattern) throws Exception {
        List<String> paths = matchingDescendants(pattern);
        List<String> answer = new ArrayList<String>();
        for (String path : paths) {
            String text = getStringData(path);
            if (Strings.isNotBlank(text)) {
                answer.add(text);
            }
        }
        return answer;
    }

    /**
     * Returns the list of all descendant paths matching the given path or pattern; where "*" can be used to indicate a wildcard folder name.
     */
    public List<String> matchingDescendants(String pattern) throws Exception {
        String[] paths = pattern.split("/?\\*/?");
        if (paths.length == 0) {
            return getAllChildren(paths[0]);
        } else if (paths.length > 0) {
            return walkDescendants(paths, 0, paths[0]);
        }
        return new ArrayList<String>();
    }

    protected List<String> walkDescendants(String[] paths, int index, String prefix) throws Exception {
        boolean isLast = index >= paths.length - 1;
        if (isLast) {
            List<String> allChildren = getAllChildren(prefix);
            if (exists(prefix) != null) {
                // we could be the only folder so lets add the prefix too
                allChildren.add(prefix);
            }
            return allChildren;
        } else {
            List<String> answer = new ArrayList<String>();
            List<String> children = getChildren(prefix);
            int nextIndex = index + 1;
            String nextPath = paths[nextIndex];
            for (String child : children) {
                String childPrefix = prefix + "/" + child + "/" + nextPath;
                List<String> childResults = walkDescendants(paths, nextIndex, childPrefix);
                answer.addAll(childResults);
            }
            return answer;
        }
    }

    /**
     * Returns the string data of the given path if it exists or null
     */
    public String getStringData(String path) throws Exception {
        return ZooKeeperUtils.getStringData(curator, path);
    }


    /**
     * Returns the list of child names for the given path or an empty list if the given path doesn't exist in curator
     */
    public List<String> getChildren(String path) throws Exception {
        return ZooKeeperUtils.getChildrenSafe(curator, path);
    }

    public List<String> getAllChildren(String path) throws Exception {
        List<String> children = getChildren(path);
        List<String> allChildren = new ArrayList<String>();
        for (String child : children) {
            String fullPath = ZKPaths.makePath(path, child);
            allChildren.add(fullPath);
            allChildren.addAll(getAllChildren(fullPath));
        }
        return allChildren;
    }



    public void create(String path) throws Exception {
        ZooKeeperUtils.create(curator, path);
    }

    public String create(String path, CreateMode createMode) throws Exception {
        return ZooKeeperUtils.create(curator, path, createMode);
    }

    public String create(String path, String data, CreateMode createMode) throws Exception {
        return ZooKeeperUtils.create(curator, path, data, createMode);
    }

    public String create(String path, byte[] data, CreateMode createMode) throws Exception {
        return ZooKeeperUtils.create(curator, path, data, createMode);
    }

    public void delete(String path) throws Exception {
        ZooKeeperUtils.deleteSafe(curator, path);
    }

    public Stat exists(String path) throws Exception {
        return ZooKeeperUtils.exists(curator, path);
    }

    /**
     * Returns the last modified time of the znode taking children into consideration.
     */
    public long lastModified(String path) throws Exception {
        long lastModified = 0;
        List<String> children = getChildren(path);
        if (children.isEmpty()) {
            return exists(path).getMtime();
        } else {
            for (String child : children) {
                lastModified = Math.max(lastModified(path + "/" + child), lastModified);
            }
        }
        return lastModified;
    }

}
