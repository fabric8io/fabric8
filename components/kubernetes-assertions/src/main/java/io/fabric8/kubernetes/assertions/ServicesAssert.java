/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.assertions;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.assertj.core.api.ListAssert;

import java.util.List;
import java.util.Objects;

import static io.fabric8.kubernetes.api.KubernetesHelper.getName;
import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class ServicesAssert extends ListAssert<Service> {
    private final KubernetesClient client;
    private final List<? extends Service> actual;

    public ServicesAssert(KubernetesClient client, List<? extends Service> actual) {
        super(actual);
        this.client = client;
        this.actual = actual;
    }

    public ServicesAssert assertAllServicesHaveEndpointOrReadyPod() {
        for (Service service : actual) {
            ServicePodsAssert asserter = new ServicePodsAssert(client, service);
            asserter.hasEndpointOrReadyPod();
        }
        return this;
    }
    
    /**
     * Asserts that the given service name exist
     *
     * @return the assertion object on the given service
     */
    public ServicePodsAssert service(String serviceName) {
        Service service = null;
        for (Service aService : actual) {
            String name = getName(aService);
            if (Objects.equals(name, serviceName)) {
                service = aService;
            }
        }
        assertThat(service).describedAs("No service could be found for name: " + serviceName).isNotNull();
        return new ServicePodsAssert(client, service);
    }
    
    
}
