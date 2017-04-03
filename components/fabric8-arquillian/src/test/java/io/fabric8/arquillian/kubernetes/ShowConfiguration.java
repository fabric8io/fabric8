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
package io.fabric8.arquillian.kubernetes;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;

import java.util.HashMap;
import java.util.Map;

import static io.fabric8.arquillian.kubernetes.Constants.FABRIC8_ENVIRONMENT;

/**
 */
public class ShowConfiguration {
    public static void main(String[] args) {
        String environmentKey = "testing";
        if (args.length > 0) {
            environmentKey = args[0];
        }
        Map<String, String> map = new HashMap<>();
        map.put(FABRIC8_ENVIRONMENT, environmentKey);

        Configuration configuration = Configuration.fromMap(map, new DefaultKubernetesClient());

        System.out.println("Namespace: " +  configuration.getNamespace());
        System.out.println("isEnvironmentInitEnabled: " +  configuration.isEnvironmentInitEnabled());
        System.out.println("isNamespaceLazyCreateEnabled: " +  configuration.isNamespaceLazyCreateEnabled());
        System.out.println("isNamespaceCleanupEnabled: " +  configuration.isNamespaceCleanupEnabled());
        System.out.println("isCreateNamespaceForTest: " +  configuration.isCreateNamespaceForTest());
    }
}
