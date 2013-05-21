package org.apache.curator.framework.recipes.cache;

import org.apache.zookeeper.data.Stat;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TreeData extends ChildData {

    private final CopyOnWriteArrayList<String> children = new CopyOnWriteArrayList<String>();

   public TreeData(String path, Stat stat, byte[] data)
    {
        super(path, stat, data);
    }

    public TreeData(String path, Stat stat, byte[] data, Collection<String> children) {
        super(path, stat, data);
        this.children.addAll(children);
    }

    public List<String> getChildren() {
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
