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
import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateWaiting;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;

import java.io.File;
import java.util.List;

/**
 */
public class LogHelpers {
    public static final String LOG_FILE_POSTFIX = ".log";

    public static File getLogFileName(File baseDir, String podName, Container container, int restartCount) {
        File logDir = new File(baseDir, "target/test-pod-logs/");
        String containerName = container.getName();
        String restartCountText = "";
        if (restartCount > 0) {
            restartCountText = "-" + restartCount;
        }
        String logFileName = podName + "-" + containerName + restartCountText + LOG_FILE_POSTFIX;
        File logFile = new File(logDir, logFileName);
        logFile.getParentFile().mkdirs();
        return logFile;
    }

    public static int getRestartCount(Pod pod) {
        int restartCount = 0;
        PodStatus podStatus = pod.getStatus();
        if (podStatus != null) {
            List<ContainerStatus> containerStatuses = podStatus.getContainerStatuses();
            for (ContainerStatus containerStatus : containerStatuses) {
                if (restartCount == 0) {
                    Integer restartCountValue = containerStatus.getRestartCount();
                    if (restartCountValue != null) {
                        restartCount = restartCountValue.intValue();
                    }
                }
            }
        }
        return restartCount;
    }
}
