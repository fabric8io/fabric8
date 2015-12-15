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
package io.fabric8.arquillian;

import io.fabric8.annotations.PodName;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Arquillian.class)
public class ServiceInjection {

    @ArquillianResource
    private KubernetesClient client;

    @ArquillianResource
    private ServiceList serviceList;

    @PodName("test-service")
    @ArquillianResource
    private Service service;


    @Test
    public void testPodListInjection() {
        assertNotNull(serviceList);
        assertEquals(1, serviceList.getItems().size());
        assertEquals("test-service", serviceList.getItems().get(0).getMetadata().getName());

        assertNotNull(service);
        assertEquals("test-service", service.getMetadata().getName());
    }
}
