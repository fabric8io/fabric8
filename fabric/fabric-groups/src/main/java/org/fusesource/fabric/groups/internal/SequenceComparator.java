package org.fusesource.fabric.groups.internal;

import org.apache.curator.utils.ZKPaths;

import java.util.Comparator;

public class SequenceComparator implements Comparator<ChildData> {
    @Override
    public int compare(ChildData left, ChildData right) {
        return left.getPath().compareTo(right.getPath());
    }
}
