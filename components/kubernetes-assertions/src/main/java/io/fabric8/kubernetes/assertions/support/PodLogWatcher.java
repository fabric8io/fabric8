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
package io.fabric8.kubernetes.assertions.support;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.assertions.support.PodWatcher;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.LogWatch;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 */
public class PodLogWatcher implements Closeable {
    private final LogWatch logWatch;

    public PodLogWatcher(PodWatcher podWatcher, String name, Pod pod, String containerName, File logFile) throws FileNotFoundException {
        KubernetesClient client = podWatcher.getClient();
        ObjectMeta metadata = pod.getMetadata();
        logFile.getParentFile().mkdirs();
        PodSpec spec = pod.getSpec();
        this.logWatch = client.pods().inNamespace(metadata.getNamespace()).withName(name).inContainer(containerName).watchLog(new FileOutputStream(logFile));
    }

    @Override
    public void close() throws IOException {
        if (logWatch != null) {
            logWatch.close();
        }
    }
}
