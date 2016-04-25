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
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.client.OpenShiftClient;

/**
 * Tests creating a build config
 */
public class DeleteBuildConfig {
    public static void main(String... args) {
        if (args.length < 1) {
            System.out.println("Usage nameOfBuildConfig");
            return;
        }
        try {
            String name = args[0];
            System.out.println("Deleting BuildConfig: " + name);

            KubernetesClient kube = new DefaultKubernetesClient();
            String namespace = kube.getNamespace();
            System.out.println("Using namespace: " + namespace);
            Controller controller = new Controller(kube);
            OpenShiftClient openshift = controller.getOpenShiftClientOrJenkinshift();
            if (openshift == null) {
                System.err.println("Cannot connect to OpenShift or Jenkinshift!");
                return;
            }
            BuildConfig buildConfig = openshift.buildConfigs().withName(name).get();
            if (buildConfig != null) {
                System.out.println("Managed to load BuildConfig with resourceVersion " + KubernetesHelper.getResourceVersion(buildConfig));
            } else {
                System.err.println("Could not find BuildConfig called: " + name);
                return;
            }
            Boolean result = openshift.buildConfigs().withName(name).delete();
            System.out.println("Deleted BuildConfig with name " + name + " result: " + result);
        } catch (Exception e) {
            System.out.println("FAILED: " + e);
            e.printStackTrace();
        }
    }
}
