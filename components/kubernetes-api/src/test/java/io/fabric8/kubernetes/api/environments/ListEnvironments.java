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
package io.fabric8.kubernetes.api.environments;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Strings;

import java.util.SortedSet;

/**
 */
public class ListEnvironments {
    public static void main(String[] args) {

        KubernetesClient kubernetesClient = new DefaultKubernetesClient();
        Environments environments;
        if (args.length > 0) {
            String namespace = args[0];
            System.out.println("Listing environments for namespace: " + namespace);
            environments = Environments.load(namespace);
        } else {
            environments = Environments.load();
        }
        String environmentKey = "testing";
        if (args.length > 1) {
            environmentKey = args[1];
        }

        System.out.println("Space namespace: " + environments.getNamespace());
        
        SortedSet<Environment> set = environments.getEnvironmentSet();
        for (Environment environment : set) {
            String onCluster = "";
            String clusterAPiServer = environment.getClusterAPiServer();
            if (Strings.isNotBlank(clusterAPiServer)) {
                onCluster += " on API server: " + clusterAPiServer;
            }
            System.out.println("Environment " + environment.getName() + " maps to namespace: " + environment.getNamespace() + onCluster);
        }

        System.out.println("Namespace for environment key: " + environmentKey + " is " + Environments.namespaceForEnvironment(environmentKey));
    }
}
