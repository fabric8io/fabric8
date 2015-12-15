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
package io.fabric8.arquillian.kubernetes.enricher;

import io.fabric8.annotations.PodName;
import io.fabric8.annotations.ReplicationControllerName;
import io.fabric8.annotations.ServiceName;

import java.lang.annotation.Annotation;

public final class EnricherUtils {

    static String getServiceName(Annotation... qualifiers) {
        for (Annotation annotation : qualifiers) {
            if (annotation instanceof ServiceName) {
                return ((ServiceName) annotation).value();
            }
        }
        return null;
    }


    static String getReplicationControllerName(Annotation... qualifiers) {
        for (Annotation annotation : qualifiers) {
            if (annotation instanceof ReplicationControllerName) {
                return ((ReplicationControllerName) annotation).value();
            }
        }
        return null;
    }


    static String getPodName(Annotation... qualifiers) {
        for (Annotation annotation : qualifiers) {
            if (annotation instanceof PodName) {
                return ((PodName) annotation).value();
            }
        }
        return null;
    }
}
