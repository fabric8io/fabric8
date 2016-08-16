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
import io.fabric8.utils.Strings;

/**
 * Tests creating a build config
 */
public class CreateBuildConfig {
    public static void main(String... args) {
        if (args.length < 1) {
            System.out.println("Usage nameOfBuildConfig");
            return;
        }
        try {
            KubernetesClient kube = new DefaultKubernetesClient();
            String name = args[0];

            String namespace = kube.getNamespace();
            if (Strings.isNullOrBlank(namespace)) {
                namespace = KubernetesHelper.defaultNamespace();
            }
            if (Strings.isNullOrBlank(namespace)) {
                namespace = "default";
            }
            System.out.println("Creating a BuildConfig for name: " + name + " in namespace: " + namespace);

            BuildConfig buildConfig = new BuildConfigBuilder().
                    withNewMetadata().withName(name).withNamespace(namespace).endMetadata().
                    withNewSpec().
                    withNewSource().withType("Git").withNewGit().withUri("http://gogs.vagrant.f8/gogsadmin/" + name + ".git").endGit().endSource().endSpec().
                    build();

            System.out.println("Creating BuildConfig: " + buildConfig);
            Controller controller = new Controller(kube);
            if (controller.getNamespace() == null) {
                controller.setNamespace(namespace);
            }
            controller.applyBuildConfig(buildConfig, "Generated!");

            System.out.println("Applied!: " + name);
        } catch (Exception e) {
            System.out.println("FAILED: " + e);
            e.printStackTrace();
        }
    }
}
