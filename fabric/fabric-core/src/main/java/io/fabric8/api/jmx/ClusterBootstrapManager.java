/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.api.jmx;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.common.util.JMXUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
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
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

        CreateEnsembleOptions.Builder builder = mapper.convertValue(options, CreateEnsembleOptions.Builder.class);

        if (profileObject != null) {
            List profiles = mapper.convertValue(profileObject, List.class);
            builder.profiles(profiles);
        }

        org.apache.felix.utils.properties.Properties userProps = null;
        try {
            userProps = new org.apache.felix.utils.properties.Properties(sysprops.getConfPath().resolve("users.properties").toFile());
        } catch (IOException e) {
            userProps = new org.apache.felix.utils.properties.Properties();
        }

        if (userProps.get(username) == null) {
            userProps.put(username, password + "," + role);
        }

        CreateEnsembleOptions answer = builder.users(userProps).withUser(username, password, role).build();
        LOG.debug("Creating ensemble with options: {}", answer);

        System.setProperty(ZkDefs.GLOBAL_RESOLVER_PROPERTY, answer.getGlobalResolver());
        System.setProperty(ZkDefs.LOCAL_RESOLVER_PROPERTY, answer.getResolver());
        System.setProperty(ZkDefs.MANUAL_IP, answer.getManualIp());
        System.setProperty(ZkDefs.BIND_ADDRESS, answer.getBindAddress());
        System.setProperty(ZkDefs.MINIMUM_PORT, "" + answer.getMinimumPort());
        System.setProperty(ZkDefs.MAXIMUM_PORT, "" + answer.getMaximumPort());

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
