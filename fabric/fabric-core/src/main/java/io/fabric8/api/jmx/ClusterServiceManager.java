package io.fabric8.api.jmx;

import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.ZooKeeperClusterService;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
/**
 * @author Stan Lewis
 */
@ThreadSafe
@Component(label = "Fabric8 ZooKeeper Cluster Manager JMX MBean", metatype = false)
public final class ClusterServiceManager extends AbstractComponent implements ClusterServiceManagerMBean {

    private static ObjectName OBJECT_NAME;
    static {
        try {
            OBJECT_NAME = new ObjectName("io.fabric8:type=ClusterServiceManager");
        } catch (MalformedObjectNameException e) {
            // ignore
        }
    }

    @Reference(referenceInterface = RuntimeProperties.class)
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<RuntimeProperties>();
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
    public String getZookeeperPassword() {
        assertValid();
        return clusterService.get().getZookeeperPassword();
    }


    @Override
    public Map<String, String> getEnsembleConfiguration() throws Exception {
        return clusterService.get().getEnsembleConfiguration();
    }

    @Override
    public void createCluster(List<String> containers) {
        assertValid();
        clusterService.get().createCluster(containers);
    }

    @Override
    public void addToCluster(List<String> containers, Map<String, Object> options) {
        assertValid();
        RuntimeProperties sysprops = runtimeProperties.get();
        CreateEnsembleOptions createEnsembleOptions = ClusterBootstrapManager.getCreateEnsembleOptions(sysprops, options);
        addToCluster(containers, createEnsembleOptions);
    }

    @Override
    public void removeFromCluster(List<String> containers, Map<String, Object> options) {
        assertValid();
        RuntimeProperties sysprops = runtimeProperties.get();
        CreateEnsembleOptions createEnsembleOptions = ClusterBootstrapManager.getCreateEnsembleOptions(sysprops, options);
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

    void bindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.bind(service);
    }

    void unbindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.unbind(service);
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
