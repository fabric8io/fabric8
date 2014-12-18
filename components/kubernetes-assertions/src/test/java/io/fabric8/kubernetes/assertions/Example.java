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

import java.util.HashMap;
import java.util.Map;

import static io.fabric8.kubernetes.assertions.Asserts.assertAssertionError;
import static io.fabric8.kubernetes.assertions.Asserts.assertThat;

/**
 */
public class Example {
    public static void main(String[] args) {
        try {
            final KubernetesClient client = new KubernetesClient();

            System.out.println("About to run test on: " + client.getAddress());

            Map<String,String> consoleLabels = new HashMap<>();
            consoleLabels.put("component", "fabric8Console");

            assertThat(client).pods().runningStatus().extracting("labels").contains(consoleLabels);

            assertAssertionError(new Block() {
                @Override
                public void invoke() throws Exception {
                    Map<String,String> badLabels = new HashMap<>();
                    badLabels.put("component", "doesNotExist");

                    assertThat(client).pods().extracting("labels").contains(badLabels);
                }
            });

            assertThat(client).replicationController("fabric8ConsoleController").hasId("fabric8ConsoleController");

            assertThat(client).podsForReplicationController("fabric8ConsoleController").runningStatus().hasSize(1).extracting("labels").contains(consoleLabels);

            assertAssertionError(new Block() {
                @Override
                public void invoke() throws Exception {
                    assertThat(client).replicationController("doesNotExist").hasId("fabric8ConsoleController");
                }
            });

            System.out.println("Done!");
        } catch (Throwable e) {
            System.out.println("Caught: " + e);
            e.printStackTrace();
        }
    }
}
