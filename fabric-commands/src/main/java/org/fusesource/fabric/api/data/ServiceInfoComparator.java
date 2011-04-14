package org.fusesource.fabric.api.data;

import java.util.Comparator;

public class ServiceInfoComparator implements Comparator<ServiceInfo> {

    public int compare(ServiceInfo serviceInfo1, ServiceInfo serviceInfo2) {
        return serviceInfo1.getId().compareTo(serviceInfo2.getId());
    }
}
