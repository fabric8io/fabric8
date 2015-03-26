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

import io.fabric8.kubernetes.api.builds.BuildFinishedEvent;
import io.fabric8.kubernetes.api.builds.BuildListener;
import io.fabric8.kubernetes.api.builds.BuildWatcher;
import io.fabric8.kubernetes.api.builds.Links;

/**
 * Triggers a build using the Java API
 */
public class WatchBuilds {
    public static void main(String... args) {
        String namespace = null;
        if (args.length > 0) {
            namespace = args[0];
        }

        String consoleLink = Links.getFabric8ConsoleLink();

        KubernetesClient client = new KubernetesClient();
        BuildListener buildListener = new BuildListener() {
            @Override
            public void onBuildFinished(BuildFinishedEvent event) {
                System.out.println("Build: " + event.getUid()
                        + " for config: " + event.getConfigName()
                        + " finished. Status: " + event.getStatus()
                        + " link: " + event.getBuildLink());
            }
        };
        BuildWatcher watcher = new BuildWatcher(client, buildListener, namespace, consoleLink);

        long pollTime = 3000;
        watcher.schedule(pollTime);

        watcher.join();
    }
}
