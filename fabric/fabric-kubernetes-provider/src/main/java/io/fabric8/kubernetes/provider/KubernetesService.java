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
package io.fabric8.kubernetes.provider;

import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.common.util.Strings;
import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.KubernetesFactory;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.util.Map;

/**
 * Represents a Kubernetes service which exposes a <a href="http://kubernetes.io/">Kubernetes</a> client
 */
@ThreadSafe
@Component(name = "io.fabric8.kubernetes",
        label = "Fabric8 Kubernetes Service",
        description = "Provides a Kubernetes client",
        policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = true)
@Service(KubernetesService.class)
public class KubernetesService extends AbstractComponent implements KubernetesServiceMXBean {

    private static final transient Logger LOG = LoggerFactory.getLogger(KubernetesService.class);

    public static Kubernetes getKubernetes(KubernetesService service) {
        return service != null ? service.getKubernetes() : null;
    }

    public static KubernetesFactory getKubernetesFactory(KubernetesService service) {
        return service != null ? service.getKubernetesFactory() : null;
    }

    public static String getKubernetesAddress(KubernetesService service) {
        KubernetesFactory factory = getKubernetesFactory(service);
        return factory != null ? factory.getAddress() : null;
    }

    @Reference(bind = "bindConfigurer", unbind = "unbindConfigurer")
    private Configurer configurer;
    @Reference(referenceInterface = MBeanServer.class, bind = "bindMBeanServer", unbind = "unbindMBeanServer")
    private MBeanServer mbeanServer;

    @Property(name = "kubernetesMaster",
            label = "Kubernetes Master",
            description = "The URL to connect to the Kubernetes Master.")
    private String kubernetesMaster;

    private KubernetesFactory kubernetesFactory;
    private Kubernetes kubernetes;
    private ObjectName objectName;

    @Activate
    public void activate(Map<String, ?> configuration) throws Exception {
        updateConfiguration(configuration);
        activateComponent();
        if (mbeanServer != null) {
            objectName = new ObjectName("io.fabric8:type=Kubernetes");
            KubernetesServiceMXBean mbean = this;
            if (!mbeanServer.isRegistered(objectName)) {
                mbeanServer.registerMBean(mbean, objectName);
            }
        } else {
            LOG.warn("No MBeanServer!");
        }
    }

    @Modified
    public void modified(Map<String, ?> configuration) throws Exception {
        updateConfiguration(configuration);
    }

    @Deactivate
    public void deactivate() throws MBeanRegistrationException, InstanceNotFoundException {
        deactivateComponent();
        if (mbeanServer != null) {
            if (mbeanServer.isRegistered(objectName)) {
                mbeanServer.unregisterMBean(objectName);
            }
        }
    }

    private void updateConfiguration(Map<String, ?> configuration) throws Exception {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            getConfigurer().configure(configuration, this);
            kubernetesFactory = new KubernetesFactory();
            if (Strings.isNotBlank(kubernetesMaster)) {
                kubernetesFactory.setAddress(kubernetesMaster);
            }
            // Resteasy uses the TCCL to load the API
            Thread.currentThread().setContextClassLoader(Kubernetes.class.getClassLoader());
            this.kubernetes = kubernetesFactory.createKubernetes();
        } catch (Throwable e) {
            LOG.error("Failed to update configuration " + configuration + ". " + e, e);
            throw e;
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    public Kubernetes getKubernetes() {
        return kubernetes;
    }

    public KubernetesFactory getKubernetesFactory() {
        return kubernetesFactory;
    }

    @Override
    public String getKubernetesAddress() {
        return kubernetesFactory.getAddress();
    }

    public Configurer getConfigurer() {
        return configurer;
    }

    public void setConfigurer(Configurer configurer) {
        this.configurer = configurer;
    }

    void bindConfigurer(Configurer configurer) {
        this.setConfigurer(configurer);
    }

    void unbindConfigurer(Configurer configurer) {
        this.setConfigurer(null);
    }

    void bindMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    void unbindMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = null;
    }
}
