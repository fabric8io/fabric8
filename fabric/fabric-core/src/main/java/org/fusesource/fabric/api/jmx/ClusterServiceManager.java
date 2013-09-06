package org.fusesource.fabric.api.jmx;

import org.apache.felix.scr.annotations.*;
import org.fusesource.fabric.api.CreateEnsembleOptions;
import org.fusesource.fabric.api.ZooKeeperClusterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.List;
import java.util.Map;

/**
 * @author Stan Lewis
 */
@Component(description = "Fabric ZooKeeper Cluster Manager JMX MBean")
public class ClusterServiceManager implements ClusterServiceManagerMBean {
    private static final transient Logger LOG = LoggerFactory.getLogger(ClusterServiceManager.class);

    @Reference
    private ZooKeeperClusterService service;

    @Reference(bind = "bindMBeanServer", unbind = "unbindMBeanServer")
    private MBeanServer mbeanServer;

    private ObjectName objectName;

    public ObjectName getObjectName() throws MalformedObjectNameException {
        if (objectName == null) {
            objectName = new ObjectName("org.fusesource.fabric:type=ClusterServiceManager");
        }
        return objectName;
    }

    public void setObjectName(ObjectName objectName) {
        this.objectName = objectName;
    }

    @Activate
    public void init() throws Exception {
        JMXUtils.registerMBean(this, mbeanServer, getObjectName());
    }

    @Deactivate
    public void destroy() throws Exception {
        JMXUtils.unregisterMBean(mbeanServer, getObjectName());
    }

    public void bindMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    public void unbindMBeanServer(MBeanServer mbeanServer) {
            this.mbeanServer = null;
    }

    @Override
    public List<String> getEnsembleContainers() {
        return service.getEnsembleContainers();
    }

    @Override
    public String getZooKeeperUrl() {
        return service.getZooKeeperUrl();
    }

    @Override
    public void createCluster(List<String> containers) {
        service.createCluster(containers);
    }

    @Override
    public void addToCluster(List<String> containers, Map<String, Object> options) {
        CreateEnsembleOptions createEnsembleOptions = ClusterBootstrapManager.getCreateEnsembleOptions(options);
        addToCluster(containers, createEnsembleOptions);
    }

    @Override
    public void removeFromCluster(List<String> containers, Map<String, Object> options) {
        CreateEnsembleOptions createEnsembleOptions = ClusterBootstrapManager.getCreateEnsembleOptions(options);
        removeFromCluster(containers, createEnsembleOptions);
    }

    @Override
    public void createCluster(List<String> containers, CreateEnsembleOptions options) {
        service.createCluster(containers, options);
    }

    @Override
    public void addToCluster(List<String> containers) {
        service.addToCluster(containers);
    }

    @Override
    public void addToCluster(List<String> containers, CreateEnsembleOptions options) {
        service.addToCluster(containers, options);
    }

    @Override
    public void removeFromCluster(List<String> containers) {
        service.removeFromCluster(containers);
    }

    @Override
    public void removeFromCluster(List<String> containers, CreateEnsembleOptions options) {
        service.removeFromCluster(containers, options);
    }

    @Override
    public void clean() {
        service.clean();
    }

    public ZooKeeperClusterService getService() {
        return this.service;
    }
}
