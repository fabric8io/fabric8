package io.fabric8.api.jmx;

import java.util.Map;

/**
 */
public interface ClusterBootstrapManagerMBean {

    public void createCluster();

    public void createCluster(Map<String, Object> options);


}
