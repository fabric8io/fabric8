/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.cdi;


import io.fabric8.kubernetes.client.KubernetesClient;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import java.util.Set;

public class KubernetesHolder {

    private static KubernetesClient client;

    public synchronized static KubernetesClient getClient() {
        if (client != null) {
            return client;
        }
        BeanManager beanManager = CDI.current().getBeanManager();

        Set<Bean<?>> beans = beanManager.getBeans(KubernetesClient.class);
        if (beans.isEmpty()) {
            throw new IllegalStateException("Could not find client beans!");
        } else {
            CreationalContext ctx = beanManager.createCreationalContext(null);
            client = (KubernetesClient) beanManager.getReference(beans.iterator().next(), KubernetesClient.class, ctx);
        }
        return client;
    }

}
