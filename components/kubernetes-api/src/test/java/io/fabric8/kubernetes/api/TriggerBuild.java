/**
 *  Copyright 2005-2014 Red Hat, Inc.
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

/**
 * Triggers a build using the Java API
 */
public class TriggerBuild {
    public static void main(String... args) {
        if (args.length < 1) {
            System.out.println("Usage: buildConfigName namespace secret type");
            return;
        }
        String name = args[0];
        String namespace = "default";
        if (args.length > 1) {
            namespace = args[1];
        }

        KubernetesClient client = new KubernetesClient();

        System.out.println("Connecting to kubernetes on: " + client.getAddress());

        try {
            String uuid = client.triggerBuildAndGetUuid(name, namespace);
            System.out.println("Build triggered: got UUID: " + uuid);
        } catch (Exception e) {
            System.out.println("FAILED: " + e);
            e.printStackTrace();
        }
    }
}
