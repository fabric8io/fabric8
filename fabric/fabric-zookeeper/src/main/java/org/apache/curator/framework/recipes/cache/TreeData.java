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
package org.apache.curator.framework.recipes.cache;

import org.apache.zookeeper.data.Stat;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class TreeData extends ChildData {

    private final Set<String> children = new HashSet<String>();
    private volatile boolean invalidated;

    public TreeData(String path, Stat stat, byte[] data)
    {
        super(path, stat, data);
    }

    public TreeData(String path, Stat stat, byte[] data, Collection<String> children) {
        super(path, stat, data);
        this.children.addAll(children);
    }

    public boolean isInvalidated() {
        return invalidated;
    }

    public void invalidate() {
        this.invalidated = true;
    }

    public Set<String> getChildren() {
        return children;
    }

    public void clearChildren() {
        children.clear();
    }

    @Override
    public String toString()
    {
        return "TreeData{" +
                "path='" + getPath() + '\'' +
                ", stat=" + getStat() +
                ", data=" + Arrays.toString(getData()) +
                ", children=" + getChildren() +
                '}';
    }
}
