/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.arquillian;

import io.fabric8.arquillian.kubernetes.ClientCreator;
import io.fabric8.arquillian.kubernetes.TestListener;
import io.fabric8.arquillian.kubernetes.Configuration;
import io.fabric8.arquillian.kubernetes.Configurer;
import io.fabric8.arquillian.kubernetes.Constants;
import io.fabric8.arquillian.kubernetes.ControllerCreator;
import io.fabric8.arquillian.kubernetes.SessionListener;
import io.fabric8.arquillian.kubernetes.SuiteListener;
import io.fabric8.arquillian.kubernetes.enricher.ClientResourceProvider;
import io.fabric8.arquillian.kubernetes.enricher.ControllerResourceProvider;
import io.fabric8.arquillian.kubernetes.enricher.J4pClientProvider;
import io.fabric8.arquillian.kubernetes.enricher.JolokiaClientsProvider;
import io.fabric8.arquillian.kubernetes.enricher.PodListResourceProvider;
import io.fabric8.arquillian.kubernetes.enricher.PodResourceProvider;
import io.fabric8.arquillian.kubernetes.enricher.ReplicationControllerListResourceProvider;
import io.fabric8.arquillian.kubernetes.enricher.ReplicationControllerResourceProvider;
import io.fabric8.arquillian.kubernetes.enricher.ServiceListResourceProvider;
import io.fabric8.arquillian.kubernetes.enricher.ServiceResourceProvider;
import io.fabric8.arquillian.kubernetes.enricher.SessionResourceProvider;
import io.fabric8.arquillian.kubernetes.log.LoggerFactory;
import io.fabric8.utils.Strings;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * An Arquillian extension for Kubernetes.
 */
public class KubernetesExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.observer(Configuration.class)
                .observer(Configurer.class)
                .observer(getClientCreator())
                .observer(ControllerCreator.class)
                .observer(LoggerFactory.class)
                .observer(SuiteListener.class)
                .observer(TestListener.class)
                .observer(SessionListener.class);

        builder.service(ResourceProvider.class, ClientResourceProvider.class)
                .service(ResourceProvider.class, ControllerResourceProvider.class)
                .service(ResourceProvider.class, JolokiaClientsProvider.class)
                .service(ResourceProvider.class, J4pClientProvider.class)
                .service(ResourceProvider.class, PodListResourceProvider.class)
                .service(ResourceProvider.class, PodResourceProvider.class)
                .service(ResourceProvider.class, ReplicationControllerListResourceProvider.class)
                .service(ResourceProvider.class, ReplicationControllerResourceProvider.class)
                .service(ResourceProvider.class, ServiceListResourceProvider.class)
                .service(ResourceProvider.class, ServiceResourceProvider.class)
                .service(ResourceProvider.class, SessionResourceProvider.class);
    }

    private Class getClientCreator() {
        Class creatorClass = null;
        String creatorClassName = System.getProperty(Constants.CLIENT_CREATOR_CLASS_NAME);
        try {
            if (Strings.isNotBlank(creatorClassName))
                creatorClass = KubernetesExtension.class.getClassLoader().loadClass(creatorClassName);
        } catch (Throwable t) {
            //fallback to default
        }
        return creatorClass != null ? creatorClass : ClientCreator.class;
    }
}
