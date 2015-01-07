/*
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.arquillian.kubernetes;

import io.fabric8.arquillian.kubernetes.log.Logger;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.utils.Filter;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a testing session.
 * It is used for scoping pods, service and replication controllers created during the test.
 */
public class Session {

    private final String id;
    private final Logger logger;

    public Session(String id, Logger logger) {
        this.id = id;
        this.logger = logger;
    }

    void init() {
        logger.status("Initializing Session:" + id);
    }

    void destroy() {
        logger.status("Destroying Session:" + id);
        System.out.flush();
    }

    public String getId() {
        return id;
    }

    public Logger getLogger() {
        return logger;
    }

    /**
     * Returns a newly created filter on pods which are created as part of this Arquillian session
     */
    public Filter<Pod> createPodFilter() {
        return new Filter<Pod>() {
            @Override
            public String toString() {
                return "ArqPodFilter(" + Constants.ARQ_KEY + " = " + getId() + ")";

            }

            @Override
            public boolean matches(Pod pod) {
                Map<String, String> labels = pod.getLabels();
                if (labels != null) {
                    String value = labels.get(Constants.ARQ_KEY);
                    return Objects.equals(value, getId());
                }
                return false;
            }
        };
    }
}
