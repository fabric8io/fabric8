package org.fusesource.fabric.api.data;

import java.util.Comparator;

public class BundleInfoComparator implements Comparator<BundleInfo> {

    public int compare(BundleInfo object1, BundleInfo object2) {
        return object1.getId().compareTo(object2.getId());
    }
}
