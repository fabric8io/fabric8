package org.fusesource.fabric.api.jmx;

import org.fusesource.fabric.api.ZooKeeperClusterService;

import java.util.List;
import java.util.Map;

/**
 * @author Stan Lewis
 */
public interface ClusterServiceManagerMBean extends ZooKeeperClusterService {

    public void createCluster(List<String> containers, Map<String, Object> options);

    public void addToCluster(List<String> containers, Map<String, Object> options);

    public void removeFromCluster(List<String> containers, Map<String, Object> options);

}
