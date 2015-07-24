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
package io.fabric8.kubernetes.api;

import io.fabric8.kubernetes.api.root.RootPaths;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.List;
import java.util.Objects;

/**
 * Tests if the current kubernetes is openshift or not
 */
public class IsOpenShift {
    public static void main(String... args) {
        KubernetesClient client = new DefaultKubernetesClient();
        try {
            boolean openShift = isOpenShift(client);
            System.out.println("OpenShift: " + openShift);
        } catch (Exception e) {
            System.out.println("FAILED: " + e);
            e.printStackTrace();
        }
    }

    private static boolean isOpenShift(KubernetesClient client) {
        RootPaths rootPaths = client.rootPaths();
        if (rootPaths != null) {
            List<String> paths  = rootPaths.getPaths();
            if (paths != null) {
                for (String path : paths) {
                    if (Objects.equals("/oapi", path) || Objects.equals("oapi", path)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
