/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.assertions;

import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.model.PodListSchema;
import io.fabric8.kubernetes.api.model.PodListSchemaAssert;
import io.fabric8.kubernetes.api.model.PodSchema;
import io.fabric8.kubernetes.api.model.ReplicationControllerListSchema;
import io.fabric8.kubernetes.api.model.ReplicationControllerListSchemaAssert;
import io.fabric8.kubernetes.api.model.ReplicationControllerSchema;
import io.fabric8.kubernetes.api.model.ServiceListSchema;
import io.fabric8.kubernetes.api.model.ServiceListSchemaAssert;
import io.fabric8.kubernetes.api.model.ServiceSchema;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.ListAssert;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import static io.fabric8.kubernetes.assertions.Assertions.assertThat;


/**
 * An assertion class for making assertions about the current pods, services and replication controllers
 * using the <a href="http://joel-costigliola.github.io/assertj">assertj library</a>
 */
public class KubernetesAssert extends AbstractAssert<KubernetesAssert, KubernetesClient> {
    private final KubernetesClient client;

    public KubernetesAssert(KubernetesClient client) {
        super(client, KubernetesAssert.class);
        this.client = client;
    }

    public PodListSchemaAssert podList() {
        PodListSchema pods = client.getPods();
        return assertThat(pods).isNotNull();
    }

    public ListAssert<PodSchema> pods() {
        PodListSchema podList = client.getPods();
        assertThat(podList).isNotNull();
        List<PodSchema> pods = podList.getItems();
        return (ListAssert<PodSchema>) assertThat(pods);
    }

    public ReplicationControllerListSchemaAssert replicationControllerList() {
        ReplicationControllerListSchema replicationControllers = client.getReplicationControllers();
        return assertThat(replicationControllers).isNotNull();
    }

    public ListAssert<ReplicationControllerSchema> replicationControllers() {
        ReplicationControllerListSchema replicationControllerList = client.getReplicationControllers();
        assertThat(replicationControllerList).isNotNull();
        List<ReplicationControllerSchema> replicationControllers = replicationControllerList.getItems();
        return (ListAssert<ReplicationControllerSchema>) assertThat(replicationControllers);
    }

    public ServiceListSchemaAssert serviceList() {
        ServiceListSchema serviceList = client.getServices();
        return assertThat(serviceList).isNotNull();
    }

    public ListAssert<ServiceSchema> services() {
        ServiceListSchema serviceList = client.getServices();
        assertThat(serviceList).isNotNull();
        List<ServiceSchema> services = serviceList.getItems();
        return (ListAssert<ServiceSchema>) assertThat(services);
    }
}
