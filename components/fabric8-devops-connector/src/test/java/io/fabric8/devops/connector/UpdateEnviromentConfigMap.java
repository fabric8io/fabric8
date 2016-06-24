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
package io.fabric8.devops.connector;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class UpdateEnviromentConfigMap {
    public static void main(String... args) {
        try {
            KubernetesClient kube = new DefaultKubernetesClient();

            System.out.println("Using namespace " + kube.getNamespace() + " on master: " + kube.getMasterUrl());

            Map<String, String> environments = new HashMap<>();
            environments.put("Testing2", "default-testing");
            environments.put("Staging2", "default-staging");

            DevOpsConnector connector = new DevOpsConnector();
            String consoleUrl = "http://fabric8.vagrant.f8/";
            Map<String, String> annotations = new HashMap<>();

            System.out.println("Starting to create/update the environment ConfigMap with " + environments);
            connector.updateEnvironmentConfigMap(environments, kube, annotations, consoleUrl);


            System.out.println("Now trying a second time!");
            connector.updateEnvironmentConfigMap(environments, kube, annotations, consoleUrl);
            System.out.println("Worked!!!");

        } catch (Exception e) {
            System.out.println("FAILED: " + e);
            e.printStackTrace();
        }
    }
}
