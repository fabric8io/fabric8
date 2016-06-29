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
package io.fabric8.kubernetes.assertions;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.utils.Block;

import java.util.HashMap;
import java.util.Map;

import static io.fabric8.utils.Asserts.assertAssertionError;
import static io.fabric8.kubernetes.assertions.Assertions.assertThat;

/**
 */
public class Example {
    public static void main(String[] args) {
        try {
            final KubernetesClient client = new DefaultKubernetesClient();


            assertThat(client).pods().runningStatus().hasSize(6);
            assertThat(client).pods().runningStatus().filterLabel("provider", "fabric8").assertSize().isGreaterThan(0);

            assertThat(client.services().inNamespace("default").withName("fabric8").get().getMetadata()).name().isEqualTo("fabric8");

            Map<String, String> consoleLabels = new HashMap<>();
            consoleLabels.put("component", "console");
            consoleLabels.put("provider", "fabric8");

            assertThat(client).podsForService("fabric8").runningStatus().extracting("metadata").extracting("labels").contains(consoleLabels);
            assertThat(client).podsForService("fabric8").runningStatus().hasSize(1).extracting("metadata").extracting("labels").contains(consoleLabels);


            assertThat(client).podsForService("fabric8").logs().doesNotContainText("Exception", "Error");
            assertThat(client).pods().logs().doesNotContainText("Exception", "Error");

            assertAssertionError(new Block() {
                @Override
                public void invoke() throws Exception {
                    try {
                        assertThat(client.services().inNamespace("default").withName("doesNotExist").get().getMetadata()).name().isEqualTo("fabric8-console-controller");
                    } catch (KubernetesClientException e) {
                        if (e.getCode() != 404) {
                            throw e;
                        } else {
                            throw new AssertionError(e);
                        }
                    }
                }
            });

            assertAssertionError(new Block() {
                @Override
                public void invoke() throws Exception {
                    try {
                        assertThat(client).pods().runningStatus().filterLabel("component", "doesNotExist").hasSize(1);
                    } catch (KubernetesClientException e) {
                        if (e.getCode() != 404) {
                            throw e;
                        } else {
                            throw new AssertionError(e);
                        }
                    }
                }
            });


            System.out.println("Done!");
        } catch (Throwable e) {
            System.out.println("Caught: " + e);
            e.printStackTrace();
        }
    }
}
