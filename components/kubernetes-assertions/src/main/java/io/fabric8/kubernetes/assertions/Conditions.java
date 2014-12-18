/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.assertions;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.PodStatus;
import io.fabric8.kubernetes.api.model.PodSchema;
import io.fabric8.kubernetes.api.model.ReplicationControllerSchema;
import io.fabric8.kubernetes.api.model.ServiceSchema;
import org.assertj.core.api.Condition;

import java.util.Map;
import java.util.Objects;

/**
 */
public class Conditions {

    public static Condition<PodSchema> status(final PodStatus status) {
        return new Condition<PodSchema>() {
            @Override
            public String toString() {
                return "podStatus(" + status + ")";
            }

            @Override
            public boolean matches(PodSchema pod) {
                return Objects.equals(status, KubernetesHelper.getPodStatus(pod));
            }
        };
    }

    public static Condition<PodSchema> runningStatus() {
        return status(PodStatus.OK);
    }

    public static Condition<PodSchema> waitingStatus() {
        return status(PodStatus.WAIT);
    }

    public static Condition<PodSchema> errorStatus() {
        return status(PodStatus.ERROR);
    }


    public static Condition<PodSchema> podLabel(final String key, final String value) {
        return new Condition<PodSchema>() {
            @Override
            public String toString() {
                return "podLabel(" + key + " = " + value + ")";
            }

            @Override
            public boolean matches(PodSchema pod) {
                return matchesLabel(pod.getLabels(), key, value);
            }
        };
    }


    public static Condition<ReplicationControllerSchema> replicationControllerLabel(final String key, final String value) {
        return new Condition<ReplicationControllerSchema>() {
            @Override
            public String toString() {
                return "replicationControllerLabel(" + key + " = " + value + ")";
            }

            @Override
            public boolean matches(ReplicationControllerSchema replicationControllerSchema) {
                return matchesLabel(replicationControllerSchema.getLabels(), key, value);
            }
        };
    }


    public static Condition<ServiceSchema> serviceLabel(final String key, final String value) {
        return new Condition<ServiceSchema>() {
            @Override
            public String toString() {
                return "serviceLabel(" + key + " = " + value + ")";
            }

            @Override
            public boolean matches(ServiceSchema service) {
                return matchesLabel(service.getLabels(), key, value);
            }
        };
    }

    public static boolean matchesLabel(Map<String, String> labels, String key, String value) {
        if (labels != null) {
            String actual = labels.get(key);
            return Objects.equals(value, actual);
        } else {
            return false;
        }
    }

}
