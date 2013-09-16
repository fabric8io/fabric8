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
import org.fusesource.fabric.service.support.AbstractComponent;
import org.fusesource.fabric.service.support.ValidatingReference;
import org.osgi.service.component.ComponentContext;

/**
 * @author Stan Lewis
 */
@Component(description = "Fabric ZooKeeper Cluster Manager JMX MBean")
public class ClusterServiceManager extends AbstractComponent implements ClusterServiceManagerMBean {

    @Reference(referenceInterface = ZooKeeperClusterService.class)
    private final ValidatingReference<ZooKeeperClusterService> service = new ValidatingReference<ZooKeeperClusterService>();

    @Reference(referenceInterface = MBeanServer.class, bind = "bindMBeanServer", unbind = "unbindMBeanServer")
    private final ValidatingReference<MBeanServer> mbeanServer = new ValidatingReference<MBeanServer>();

    private ObjectName objectName;

    @Activate
    synchronized void activate(ComponentContext context) throws Exception {
        activateComponent(context);
        try {
            JMXUtils.registerMBean(this, mbeanServer.get(), getObjectName());
        } catch (Exception ex) {
            deactivateComponent();
            throw ex;
        }
    }

    @Deactivate
    synchronized void deactivate() throws Exception {
        try {
            JMXUtils.unregisterMBean(mbeanServer.get(), getObjectName());
        } finally {
            deactivateComponent();
        }
    }

    private ObjectName getObjectName() throws MalformedObjectNameException {
        if (objectName == null) {
            objectName = new ObjectName("org.fusesource.fabric:type=ClusterServiceManager");
        }
        return objectName;
    }

    @Override
    public List<String> getEnsembleContainers() {
        return service.get().getEnsembleContainers();
    }

    @Override
    public String getZooKeeperUrl() {
        return service.get().getZooKeeperUrl();
    }

    @Override
    public void createCluster(List<String> containers) {
        service.get().createCluster(containers);
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
        service.get().createCluster(containers, options);
    }

    @Override
    public void addToCluster(List<String> containers) {
        service.get().addToCluster(containers);
    }

    @Override
    public void addToCluster(List<String> containers, CreateEnsembleOptions options) {
        service.get().addToCluster(containers, options);
    }

    @Override
    public void removeFromCluster(List<String> containers) {
        service.get().removeFromCluster(containers);
    }

    @Override
    public void removeFromCluster(List<String> containers, CreateEnsembleOptions options) {
        service.get().removeFromCluster(containers, options);
    }

    @Override
    public void clean() {
        service.get().clean();
    }

    void bindMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer.set(mbeanServer);
    }

    void unbindMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer.set(null);
    }

    void bindService(ZooKeeperClusterService service) {
        this.service.set(service);
    }

    void unbindService(ZooKeeperClusterService service) {
        this.service.set(null);
    }
}
