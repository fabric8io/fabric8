/*
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.arquillian.kubernetes.enricher;

import io.fabric8.arquillian.kubernetes.Constants;
import io.fabric8.arquillian.kubernetes.Session;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ServiceListSchema;
import io.fabric8.kubernetes.api.model.ServiceSchema;
import io.fabric8.utils.Filter;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A {@link org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider} for {@link io.fabric8.kubernetes.api.model.ServiceListSchema}.
 * It refers to services that have been created during the current session.
 */
public class ServiceListResourceProvider implements ResourceProvider {

    @Inject
    private Instance<KubernetesClient> clientInstance;

    @Inject
    private Instance<Session> sessionInstance;

    @Override
    public boolean canProvide(Class<?> type) {
        return ServiceListSchema.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        KubernetesClient client = this.clientInstance.get();
        Session session = sessionInstance.get();

        Map<String, String> labels = Collections.singletonMap(Constants.ARQ_KEY, session.getId());
        Filter<ServiceSchema> serviceFilter = KubernetesHelper.createServiceFilter(labels);
        ServiceListSchema services = client.getServices();
        List<ServiceSchema> sessionServices = new ArrayList<>();

        for (ServiceSchema service : client.getServices().getItems()) {
            if (serviceFilter.matches(service)) {
                sessionServices.add(service);
            }
        }
        services.setItems(sessionServices);
        return services;
    }
}
