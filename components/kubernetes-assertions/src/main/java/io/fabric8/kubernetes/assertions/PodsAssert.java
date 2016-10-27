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
package io.fabric8.kubernetes.assertions;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodAssert;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Asserts;
import io.fabric8.utils.Strings;
import org.assertj.core.api.Condition;
import org.assertj.core.api.IntegerAssert;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.ObjectEnumerableAssert;
import org.assertj.core.api.filter.Filters;
import org.assertj.core.util.Lists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.fabric8.kubernetes.assertions.Conditions.podLabel;
import static io.fabric8.kubernetes.assertions.Conditions.podNamespace;

/**
 * Adds some extra assertion operations
 */
public class PodsAssert extends HasMetadatasAssert<Pod, PodsAssert> {
    private final KubernetesClient client;

    public PodsAssert(List<Pod> actual, KubernetesClient client) {
        super(actual);
        this.client = client;
    }

    /**
     * Returns an assertion on the logs of the pods
     */
    public PodLogsAssert logs() {
        return logs(null);
    }

    /**
     * Returns an assertion on the logs of the pods for the given container name
     */
    public PodLogsAssert logs(String containerName) {
        Map<String,String> logs = new HashMap<>();
        List<Pod> pods = get();
        for (Pod pod : pods) {
            ObjectMeta metadata = pod.getMetadata();
            if (metadata != null) {
                String name = metadata.getName();
                String namespace = metadata.getNamespace();
                String key = name;
                if (Strings.isNotBlank(namespace)) {
                    key = namespace + "/" + name;
                }
                // TODO deal with more than one container!!!
                //String log = client.pods().inNamespace(namespace).withName(name).getLog(containerName, true);
                String log = client.pods().inNamespace(namespace).withName(name).getLog(true);
                if (log != null) {
                    logs.put(key, log);
                }
            }
        }
        return new PodLogsAssert(logs, containerName);
    }

    /**
     * Returns the filtered list of pods which have running status
     */
    public PodsAssert runningStatus() {
        return filter(Conditions.runningStatus());
    }

    /**
     * Returns the filtered list of pods which have waiting status
     */
    public PodsAssert waitingStatus() {
        return filter(Conditions.waitingStatus());
    }

    /**
     * Returns the filtered list of pods which have error status
     */
    public PodsAssert errorStatus() {
        return filter(Conditions.errorStatus());
    }

    @Override
    protected PodsAssert createListAssert(List<Pod> list) {
        return new PodsAssert(list, client);
    }
}
