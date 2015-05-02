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

import io.fabric8.kubernetes.api.model.Capabilities;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerManifest;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodState;
import io.fabric8.kubernetes.api.model.PodTemplate;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerState;
import io.fabric8.kubernetes.api.model.RestartPolicy;
import io.fabric8.kubernetes.api.model.RestartPolicyAlways;
import io.fabric8.kubernetes.api.model.RestartPolicyNever;
import io.fabric8.kubernetes.api.model.RestartPolicyOnFailure;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeSource;
import io.fabric8.kubernetes.api.model.util.IntOrString;
import io.fabric8.utils.Objects;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper methods to compare the user configuration on entities
 */
public class ConfigurationCompare {
    /**
     * Returns true if the user configured metadata is equal
     */
    public static boolean configEqual(Service entity1, Service entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null || entity2 == null) {
            return false;
        }
        return configEqual(entity1.getLabels(), entity2.getLabels()) &&
                configEqual(entity1.getAnnotations(), entity2.getAnnotations()) &&
                configEqual(entity1.getSelector(), entity2.getSelector()) &&
                configEqual(entity1.getContainerPort(), entity2.getContainerPort()) &&
                configEqual(entity1.getCreateExternalLoadBalancer(), entity2.getCreateExternalLoadBalancer()) &&
                configEqual(entity1.getPort(), entity2.getPort()) &&
                configEqual(entity1.getSessionAffinity(), entity2.getSessionAffinity());
    }

    /**
     * Returns true if the user configured metadata is equal
     */
    public static boolean configEqual(ReplicationController entity1, ReplicationController entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null || entity2 == null) {
            return false;
        }
        return configEqual(entity1.getLabels(), entity2.getLabels()) &&
                configEqual(entity1.getAnnotations(), entity2.getAnnotations()) &&
                configEqual(entity1.getLabels(), entity2.getLabels()) &&
                configEqual(entity1.getDesiredState(), entity2.getDesiredState());
    }

    public static boolean configEqual(ReplicationControllerState entity1, ReplicationControllerState entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null || entity2 == null) {
            return false;
        }
        return configEqual(entity1.getReplicas(), entity2.getReplicas()) &&
                configEqual(entity1.getReplicaSelector(), entity2.getReplicaSelector()) &&
                configEqual(entity1.getPodTemplate(), entity2.getPodTemplate());
    }

    public static boolean configEqual(PodTemplate entity1, PodTemplate entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null || entity2 == null) {
            return false;
        }
        return configEqual(entity1.getDesiredState(), entity2.getDesiredState()) &&
                configEqual(entity1.getAnnotations(), entity2.getAnnotations()) &&
                configEqual(entity1.getLabels(), entity2.getLabels()) &&
                configEqual(entity1.getNodeSelector(), entity2.getNodeSelector());
    }

    public static boolean configEqual(Pod entity1, Pod entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null || entity2 == null) {
            return false;
        }
        return configEqual(entity1.getDesiredState(), entity2.getDesiredState()) &&
                configEqual(entity1.getAnnotations(), entity2.getAnnotations()) &&
                configEqual(entity1.getLabels(), entity2.getLabels()) &&
                configEqual(entity1.getNodeSelector(), entity2.getNodeSelector());
    }

    public static boolean configEqual(PodState entity1, PodState entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null || entity2 == null) {
            return false;
        }
        return configEqual(entity1.getManifest(), entity2.getManifest());
    }

    public static boolean configEqual(ContainerManifest entity1, ContainerManifest entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null || entity2 == null) {
            return false;
        }
        return configEqual(entity1.getContainers(), entity2.getContainers()) &&
                configEqual(entity1.getHostNetwork(), entity2.getHostNetwork()) &&
                configEqual(entity1.getRestartPolicy(), entity2.getRestartPolicy()) &&
                configEqual(entity1.getVolumes(), entity2.getVolumes());
    }

    public static boolean configEqual(Container entity1, Container entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null || entity2 == null) {
            return false;
        }
        return configEqual(entity1.getCapabilities(), entity2.getCapabilities()) &&
                configEqual(entity1.getCommand(), entity2.getCommand()) &&
                configEqual(entity1.getCpu(), entity2.getCpu()) &&
                configEqual(entity1.getEnv(), entity2.getEnv()) &&
                configEqual(entity1.getImage(), entity2.getImage()) &&
                configEqual(entity1.getImagePullPolicy(), entity2.getImagePullPolicy()) &&
                configEqual(entity1.getLivenessProbe(), entity2.getLivenessProbe()) &&
                configEqual(entity1.getMemory(), entity2.getMemory()) &&
                configEqual(entity1.getName(), entity2.getName()) &&
                configEqual(entity1.getPrivileged(), entity2.getPrivileged()) &&
                configEqual(entity1.getPorts(), entity2.getPorts()) &&
                configEqual(entity1.getVolumeMounts(), entity2.getVolumeMounts());
    }

    public static boolean configEqual(EnvVar entity1, EnvVar entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null || entity2 == null) {
            return false;
        }
        return configEqual(entity1.getName(), entity2.getName()) &&
                configEqual(entity1.getValue(), entity2.getValue());
    }

    public static boolean configEqual(Volume entity1, Volume entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null || entity2 == null) {
            return false;
        }
        return configEqual(entity1.getName(), entity2.getName()) &&
                configEqual(entity1.getSource(), entity2.getSource());
    }

    public static boolean configEqual(VolumeSource entity1, VolumeSource entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null || entity2 == null) {
            return false;
        }
        return configEqual(entity1.getEmptyDir(), entity2.getEmptyDir()) &&
                configEqual(entity1.getGitRepo(), entity2.getGitRepo()) &&
                configEqual(entity1.getGlusterfs(), entity2.getGlusterfs()) &&
                configEqual(entity1.getHostDir(), entity2.getHostDir()) &&
                configEqual(entity1.getIscsi(), entity2.getIscsi()) &&
                configEqual(entity1.getNfs(), entity2.getNfs()) &&
                configEqual(entity1.getPersistentDisk(), entity2.getPersistentDisk()) &&
                configEqual(entity1.getSecret(), entity2.getSecret());
    }

    public static boolean configEqual(RestartPolicy entity1, RestartPolicy entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null || entity2 == null) {
            return false;
        }
        return configEqual(entity1.getAlways(), entity2.getAlways()) &&
                configEqual(entity1.getNever(), entity2.getNever()) &&
                configEqual(entity1.getOnFailure(), entity2.getOnFailure());
    }

    public static boolean configEqual(RestartPolicyAlways entity1, RestartPolicyAlways entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null || entity2 == null) {
            return false;
        }
        return true;
    }

    public static boolean configEqual(RestartPolicyNever entity1, RestartPolicyNever entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null || entity2 == null) {
            return false;
        }
        return true;
    }

    public static boolean configEqual(RestartPolicyOnFailure entity1, RestartPolicyOnFailure entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null || entity2 == null) {
            return false;
        }
        return true;
    }

    public static boolean configEqual(Capabilities entity1, Capabilities entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null || entity2 == null) {
            return false;
        }
        return configEqual(entity1.getAdd(), entity2.getAdd()) &&
                configEqual(entity1.getDrop(), entity2.getDrop());
    }


    public static boolean configEqual(IntOrString entity1, IntOrString entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null || entity2 == null) {
            return false;
        }
        return configEqual(entity1.getKind(), entity2.getKind()) &&
                configEqual(entity1.getIntVal(), entity2.getIntVal()) &&
                configEqual(entity1.getStrVal(), entity2.getStrVal());
    }

    public static boolean configEqual(Map<String, String> entity1, Map<String, String> entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null || entity2 == null) {
            return false;
        }
        int size1 = size(entity1);
        int size2 = size(entity2);
        if (size1 != size2) {
            return false;
        }
        Set<Map.Entry<String, String>> entries = entity1.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            String value = entry.getValue();
            String value2 = entity2.get(key);
            if (!configEqual(value, value2)) {
                return false;
            }
        }
        return true;
    }

    public static boolean configEqual(List v1, List v2) {
        int size1 = size(v1);
        int size2 = size(v2);
        if (size1 != size2) {
            return false;
        }
        int idx = 0;
        for (Object value : v1) {
            Object value2 = v2.get(idx++);
            if (!configEqual(value, value2)) {
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

    protected static int size(Map map) {
        return (map == null) ? 0 : map.size();
    }

    protected static int size(Collection coll) {
        return (coll == null) ? 0 : coll.size();
    }

    public static boolean configEqual(Object entity1, Object entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null) {
            return configEqual(entity2, entity1);
        } else if (entity1 instanceof Service) {
            return configEqual((Service) entity1, cast(Service.class, entity2));
        } else if (entity1 instanceof ReplicationController) {
            return configEqual((ReplicationController) entity1, cast(ReplicationController.class, entity2));
        } else if (entity1 instanceof ReplicationControllerState) {
            return configEqual((ReplicationControllerState) entity1, cast(ReplicationControllerState.class, entity2));
        } else if (entity1 instanceof PodTemplate) {
            return configEqual((PodTemplate) entity1, cast(PodTemplate.class, entity2));
        } else if (entity1 instanceof Pod) {
            return configEqual((Pod) entity1, cast(Pod.class, entity2));
        } else if (entity1 instanceof PodState) {
            return configEqual((PodState) entity1, cast(PodState.class, entity2));
        } else if (entity1 instanceof ContainerManifest) {
            return configEqual((ContainerManifest) entity1, cast(ContainerManifest.class, entity2));
        } else if (entity1 instanceof Container) {
            return configEqual((Container) entity1, cast(Container.class, entity2));
        } else if (entity1 instanceof Volume) {
            return configEqual((Volume) entity1, cast(Volume.class, entity2));
        } else if (entity1 instanceof EnvVar) {
            return configEqual((EnvVar) entity1, cast(EnvVar.class, entity2));
        } else if (entity1 instanceof VolumeSource) {
            return configEqual((VolumeSource) entity1, cast(VolumeSource.class, entity2));
        } else if (entity1 instanceof RestartPolicy) {
            return configEqual((RestartPolicy) entity1, cast(RestartPolicy.class, entity2));
        } else if (entity1 instanceof RestartPolicyAlways) {
            return configEqual((RestartPolicyAlways) entity1, cast(RestartPolicyAlways.class, entity2));
        } else if (entity1 instanceof RestartPolicyNever) {
            return configEqual((RestartPolicyNever) entity1, cast(RestartPolicyNever.class, entity2));
        } else if (entity1 instanceof RestartPolicyOnFailure) {
            return configEqual((RestartPolicyOnFailure) entity1, cast(RestartPolicyOnFailure.class, entity2));
        } else if (entity1 instanceof Capabilities) {
            return configEqual((Capabilities) entity1, cast(Capabilities.class, entity2));
        } else if (entity1 instanceof IntOrString) {
            return configEqual((IntOrString) entity1, cast(IntOrString.class, entity2));
        } else if (entity1 instanceof Map) {
            return configEqual((Map) entity1, cast(Map.class, entity2));
        } else if (entity1 instanceof List) {
            return configEqual((List) entity1, cast(List.class, entity2));
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
