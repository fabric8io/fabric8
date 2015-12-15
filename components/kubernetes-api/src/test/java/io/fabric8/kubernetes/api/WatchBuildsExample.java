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

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;

public class WatchBuildsExample {

    public static void main(String... args) throws Exception {

        OpenShiftClient client = new DefaultOpenShiftClient();
        client.builds().watch(new Watcher<Build>() {
            @Override
            public void eventReceived(Action action, Build build) {
                System.out.println(action + ": " + build);
            }

            @Override
            public void onClose(KubernetesClientException e) {
                System.out.println("Closed: " + e);
            }
        });
        client.close();
    }

}
