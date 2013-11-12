package org.fusesource.fabric.api.jmx;

import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.fusesource.fabric.api.CreateEnsembleOptions;
import org.fusesource.fabric.api.ZooKeeperClusterService;
import org.fusesource.fabric.api.jcip.ThreadSafe;
import org.fusesource.fabric.api.scr.AbstractComponent;
import org.fusesource.fabric.api.scr.ValidatingReference;
/**
 * @author Stan Lewis
 */
@ThreadSafe
@Component(description = "Fabric ZooKeeper Cluster Manager JMX MBean")
public final class ClusterServiceManager extends AbstractComponent implements ClusterServiceManagerMBean {

    private static ObjectName OBJECT_NAME;
    static {
        try {
            OBJECT_NAME = new ObjectName("org.fusesource.fabric:type=ClusterServiceManager");
        } catch (MalformedObjectNameException e) {
            // ignore
        }
    }

    @Reference(referenceInterface = ZooKeeperClusterService.class)
    private final ValidatingReference<ZooKeeperClusterService> clusterService = new ValidatingReference<ZooKeeperClusterService>();
    @Reference(referenceInterface = MBeanServer.class, bind = "bindMBeanServer", unbind = "unbindMBeanServer")
    private final ValidatingReference<MBeanServer> mbeanServer = new ValidatingReference<MBeanServer>();

    @Activate
    void activate() throws Exception {
        JMXUtils.registerMBean(this, mbeanServer.get(), OBJECT_NAME);
        activateComponent();
    }

    @Deactivate
    void deactivate() throws Exception {
        deactivateComponent();
        JMXUtils.unregisterMBean(mbeanServer.get(), OBJECT_NAME);
    }

    @Override
    public List<String> getEnsembleContainers() {
        assertValid();
        return clusterService.get().getEnsembleContainers();
    }

    @Override
    public String getZooKeeperUrl() {
        assertValid();
        return clusterService.get().getZooKeeperUrl();
    }

    @Override
    public void createCluster(List<String> containers) {
        assertValid();
        clusterService.get().createCluster(containers);
    }

    @Override
    public void addToCluster(List<String> containers, Map<String, Object> options) {
        assertValid();
        CreateEnsembleOptions createEnsembleOptions = ClusterBootstrapManager.getCreateEnsembleOptions(options);
        addToCluster(containers, createEnsembleOptions);
    }

    @Override
    public void removeFromCluster(List<String> containers, Map<String, Object> options) {
        assertValid();
        CreateEnsembleOptions createEnsembleOptions = ClusterBootstrapManager.getCreateEnsembleOptions(options);
        removeFromCluster(containers, createEnsembleOptions);
    }

    @Override
    public void createCluster(List<String> containers, CreateEnsembleOptions options) {
        assertValid();
        clusterService.get().createCluster(containers, options);
    }

    @Override
    public void addToCluster(List<String> containers) {
        assertValid();
        clusterService.get().addToCluster(containers);
    }

    @Override
    public void addToCluster(List<String> containers, CreateEnsembleOptions options) {
        assertValid();
        clusterService.get().addToCluster(containers, options);
    }

    @Override
    public void removeFromCluster(List<String> containers) {
        assertValid();
        clusterService.get().removeFromCluster(containers);
    }

    @Override
    public void removeFromCluster(List<String> containers, CreateEnsembleOptions options) {
        assertValid();
        clusterService.get().removeFromCluster(containers, options);
    }

    @Override
    public void clean() {
        assertValid();
        clusterService.get().clean();
    }

    void bindMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer.bind(mbeanServer);
    }

    void unbindMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer.unbind(mbeanServer);
    }

    void bindClusterService(ZooKeeperClusterService service) {
        this.clusterService.bind(service);
    }

    void unbindClusterService(ZooKeeperClusterService service) {
        this.clusterService.unbind(service);
    }
}
