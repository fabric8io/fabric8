/**
 * Copyright 2005-2016 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.kubernetes.api;

import io.fabric8.kubernetes.api.extensions.Configs;
import io.fabric8.kubernetes.api.model.Config;
import io.fabric8.kubernetes.api.model.Context;

import static io.fabric8.kubernetes.api.extensions.Configs.currentUserName;

/**
 * View the current config details
 */
public class ShowConfig {

    public static void main(String... args) {
        try {
            System.out.println("Current username: " + currentUserName());

            Config config = Configs.parseConfigs();
            if (config == null) {
                System.out.println("No Config loaded!");
            } else {
                Context context = Configs.getCurrentContext(config);
                if (context != null) {
                    System.out.println("context:   " + config.getCurrentContext());
                    System.out.println("cluster:   " + context.getCluster());
                    System.out.println("user:      " + context.getUser());
                    System.out.println("namespace: " + context.getNamespace());
                } else {
                    System.out.println("No current context!");
                }
            }
        } catch (Exception e) {
            System.out.println("Caught: " + e);
            e.printStackTrace();
        }
    }
}
