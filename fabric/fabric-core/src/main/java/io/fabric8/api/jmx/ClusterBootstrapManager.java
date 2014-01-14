package io.fabric8.api.jmx;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.api.FabricException;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.ZooKeeperClusterBootstrap;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.zookeeper.ZkDefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Stan Lewis
 */
@ThreadSafe
@Component(label = "Fabric8 ZooKeeper Cluster Bootstrap Manager JMX MBean", metatype = false)
public final class ClusterBootstrapManager extends AbstractComponent implements ClusterBootstrapManagerMBean {

    private static final transient Logger LOG = LoggerFactory.getLogger(ClusterBootstrapManager.class);

    private static ObjectName OBJECT_NAME;
    static {
        try {
            OBJECT_NAME = new ObjectName("io.fabric8:type=ClusterBootstrapManager");
        } catch (MalformedObjectNameException e) {
            // ignore
        }
    }

    @Reference(referenceInterface = RuntimeProperties.class, bind = "bindRuntimeProperties", unbind = "unbindRuntimeProperties")
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<RuntimeProperties>();
    @Reference(referenceInterface = ZooKeeperClusterBootstrap.class, bind = "bindBootstrap", unbind = "unbindBootstrap")
    private final ValidatingReference<ZooKeeperClusterBootstrap> bootstrap = new ValidatingReference<ZooKeeperClusterBootstrap>();
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

    static CreateEnsembleOptions getCreateEnsembleOptions(RuntimeProperties sysprops, Map<String, Object> options) {
        String username = (String) options.remove("username");
        String password = (String) options.remove("password");
        String role = (String) options.remove("role");

        if (username == null || password == null || role == null) {
            throw new FabricException("Must specify an administrator username, password and administrative role when creating a fabric");
        }

        Object profileObject = options.remove("profiles");

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

        CreateEnsembleOptions.Builder builder = mapper.convertValue(options, CreateEnsembleOptions.Builder.class);

        if (profileObject != null) {
            List profiles = mapper.convertValue(profileObject, List.class);
            builder.profiles(profiles);
        }

        org.apache.felix.utils.properties.Properties userProps = null;
        try {
            userProps = new org.apache.felix.utils.properties.Properties(new File(sysprops.getProperty("karaf.home") + "/etc/users.properties"));
        } catch (IOException e) {
            userProps = new org.apache.felix.utils.properties.Properties();
        }

        if (userProps.get(username) == null) {
            userProps.put(username, password + "," + role);
        }

        CreateEnsembleOptions answer = builder.users(userProps).withUser(username, password, role).build();
        LOG.debug("Creating ensemble with options: {}", answer);

        sysprops.setProperty(ZkDefs.GLOBAL_RESOLVER_PROPERTY, answer.getGlobalResolver());
        sysprops.setProperty(ZkDefs.LOCAL_RESOLVER_PROPERTY, answer.getResolver());
        sysprops.setProperty(ZkDefs.MANUAL_IP, answer.getManualIp());
        sysprops.setProperty(ZkDefs.BIND_ADDRESS, answer.getBindAddress());
        sysprops.setProperty(ZkDefs.MINIMUM_PORT, "" + answer.getMinimumPort());
        sysprops.setProperty(ZkDefs.MAXIMUM_PORT, "" + answer.getMaximumPort());

        return answer;
    }

    @Override
    public void createCluster() {
        assertValid();
        RuntimeProperties sysprops = runtimeProperties.get();
        bootstrap.get().create(CreateEnsembleOptions.builder().fromRuntimeProperties(sysprops).build());
    }

    @Override
    public void createCluster(Map<String, Object> options) {
        assertValid();
        RuntimeProperties sysprops = runtimeProperties.get();
        CreateEnsembleOptions createEnsembleOptions = ClusterBootstrapManager.getCreateEnsembleOptions(sysprops, options);
        bootstrap.get().create(createEnsembleOptions);
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

    void bindBootstrap(ZooKeeperClusterBootstrap bootstrap) {
        this.bootstrap.bind(bootstrap);
    }

    void unbindBootstrap(ZooKeeperClusterBootstrap bootstrap) {
        this.bootstrap.unbind(bootstrap);
    }
}
