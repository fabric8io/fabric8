package io.fabric8.api.jmx;

import io.fabric8.api.ZooKeeperClusterService;

import java.util.List;
import java.util.Map;

/**
 * @author Stan Lewis
 */
public interface ClusterServiceManagerMBean extends ZooKeeperClusterService {

    public void addToCluster(List<String> containers, Map<String, Object> options);

    public void removeFromCluster(List<String> containers, Map<String, Object> options);

}
