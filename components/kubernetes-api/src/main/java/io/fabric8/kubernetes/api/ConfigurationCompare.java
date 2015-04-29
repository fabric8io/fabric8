/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.api;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.util.IntOrString;
import io.fabric8.utils.Objects;

import java.util.Map;
import java.util.Set;

/**
 * Helper methods to compare the user configuration on entities
 */
public class ConfigurationCompare {
    /**
     * Returns true if the service metadata has changed
     */
    public static boolean configEqual(Service entity1, Service entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null || entity2 == null) {
            return false;
        }
        return configEqual(entity1.getLabels(), entity2.getLabels()) ||
                configEqual(entity1.getAnnotations(), entity2.getAnnotations()) ||
                configEqual(entity1.getLabels(), entity2.getLabels()) ||
                configEqual(entity1.getContainerPort(), entity2.getContainerPort()) ||
                configEqual(entity1.getCreateExternalLoadBalancer(), entity2.getCreateExternalLoadBalancer()) ||
                configEqual(entity1.getPort(), entity2.getPort()) ||
                configEqual(entity1.getSessionAffinity(), entity2.getSessionAffinity());
    }

    public static boolean configEqual(IntOrString entity1, IntOrString entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null || entity2 == null) {
            return false;
        }
        return configEqual(entity1.getKind(), entity2.getKind()) ||
        configEqual(entity1.getIntVal(), entity2.getIntVal()) ||
        configEqual(entity1.getStrVal(), entity2.getStrVal());
    }

    public static boolean configEqual(Map<String, String> map1, Map<String, String> map2) {
        int size1 = size(map1);
        int size2 = size(map2);
        if (size1 != size2) {
            return false;
        }
        Set<Map.Entry<String, String>> entries = map1.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            String value = entry.getValue();
            String value2 = map2.get(key);
            if (!Objects.equal(value, value2)) {
                return false;
            }
        }
        return true;
    }


    public static boolean configEqual(String v1, String v2) {
        return Objects.equal(v1, v2);
    }

    public static boolean configEqual(Boolean v1, Boolean v2) {
        return Objects.equal(v1, v2);
    }

    public static boolean configEqual(Number v1, Number v2) {
        return Objects.equal(v1, v2);
    }

    protected static int size(Map<String, String> map) {
        return (map == null) ? 0 : map.size();
    }

    public static boolean configEqual(Object entity1, Object entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null) {
            return configEqual(entity2, entity1);
        } else if (entity1 instanceof Service) {
            return configEqual((Service) entity1, cast(Service.class, entity2));
        } else if (entity1 instanceof IntOrString) {
            return configEqual((IntOrString) entity1, cast(IntOrString.class, entity2));
        } else if (entity1 instanceof Map) {
            return configEqual((Map) entity1, cast(Map.class, entity2));
        } else if (entity1 instanceof Number) {
            return configEqual((Number) entity1, cast(Number.class, entity2));
        } else if (entity1 instanceof Boolean) {
            return configEqual((Boolean) entity1, cast(Boolean.class, entity2));
        } else if (entity1 instanceof String) {
            return configEqual((String) entity1, cast(String.class, entity2));
        } else {
            throw new IllegalArgumentException("Unsupported type " + entity1.getClass().getName());
        }
    }

    private static <T> T cast(Class<T> clazz, Object entity) {
        if (entity == null) {
            return null;
        }
        if (clazz.isInstance(entity)) {
            return clazz.cast(entity);
        } else {
            throw new IllegalArgumentException("Invalid entity should be of type: " + clazz.getName()
                    + " but was " + entity.getClass().getName());
        }
    }
}
