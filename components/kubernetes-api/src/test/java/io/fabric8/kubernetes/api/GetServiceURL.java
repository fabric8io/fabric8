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
package io.fabric8.kubernetes.api;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Strings;

/**
 * Tests generating an external service URL
 */
public class GetServiceURL {
    public static void main(String... args) {
        if (args.length < 1) {
            System.out.println("Usage nameOfService");
            return;
        }
        try {
            String name = args[0];
            KubernetesClient kube = new DefaultKubernetesClient();

            String namespace = kube.getNamespace();
            if (Strings.isNullOrBlank(namespace)) {
                namespace = "default";
            }
            String url = KubernetesHelper.getServiceURL(kube, name, namespace, "http", true);
            System.out.println("Service " + name + " has external URL: " + url);
        } catch (Exception e) {
            System.out.println("FAILED: " + e);
            e.printStackTrace();
        }
    }
}
