package org.fusesource.fabric.groups.internal;

import org.apache.curator.utils.ZKPaths;

import java.util.Comparator;

public class SequenceComparator implements Comparator<ChildData> {

    private static final String SEQUENCE_PREFIX = "[a-zA-Z0-9_\\-]*-";

    @Override
    public int compare(ChildData left, ChildData right) {
        String leftSequence = ZKPaths.getNodeFromPath(left.getPath()).replaceAll(SEQUENCE_PREFIX, "");
        String rightSequence = ZKPaths.getNodeFromPath(right.getPath()).replaceAll(SEQUENCE_PREFIX, "");

        long leftValue = Long.parseLong(leftSequence);
        long rightValue = Long.parseLong(rightSequence);
        if (leftValue < rightValue) {
            return -1;
        } else if (leftValue == rightValue) {
            return 0;
        } else return 1;
    }
}
