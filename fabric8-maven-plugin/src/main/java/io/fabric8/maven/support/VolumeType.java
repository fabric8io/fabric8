/*
 * Copyright 2005-2016 Red Hat, Inc.
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

package io.fabric8.maven.support;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public enum VolumeType {

    HOST_PATH("hostPath") {
        @Override
        public Volume fromProperties(String name, Properties properties) {
            String path = properties.getProperty(String.format(VOLUME_PROPERTY, name, getType()));
            return new VolumeBuilder()
                    .withName(name)
                    .withNewHostPath(path)
                    .build();
        }
    }, EMPTY_DIR("emptyDir") {
        @Override
        public Volume fromProperties(String name, Properties properties) {
            String medium = properties.getProperty(String.format(VOLUME_PROPERTY, name, getType()));
            return new VolumeBuilder()
                    .withName(name)
                    .withNewEmptyDir(medium)
                    .build();
        }
    }, GIT_REPO("gitRepo") {
        public Volume fromProperties(String name, Properties properties) {
            String repository = properties.getProperty(String.format(VOLUME_PROPERTY, name, getType()));
            String revision = properties.getProperty(String.format(VOLUME_PROPERTY, name, VOLUME_GIT_REV));
            return new VolumeBuilder()
                    .withName(name)
                    .withNewGitRepo().withRepository(repository).withRevision(revision).endGitRepo()
                    .build();
        }
    }, SECRET("secret") {
        public Volume fromProperties(String name, Properties properties) {
            String secretName = properties.getProperty(String.format(VOLUME_PROPERTY, name, VOLUME_SECRET_NAME));
            return new VolumeBuilder()
                    .withName(name)
                    .withNewSecret().withSecretName(secretName).endSecret()
                    .build();
        }
    }, NFS_PATH("nfsPath") {
        public Volume fromProperties(String name, Properties properties) {
            String path = properties.getProperty(String.format(VOLUME_PROPERTY, name, getType()));
            String server = properties.getProperty(String.format(VOLUME_PROPERTY, name, VOLUME_NFS_SERVER));
            Boolean readOnly = toBool(properties.getProperty(String.format(VOLUME_PROPERTY, name, READONLY)));
            return new VolumeBuilder()
                    .withName(name)
                    .withNewNfs(path, readOnly, server)
                    .build();
        }
    }, CGE_DISK("gcePdName") {
        public Volume fromProperties(String name, Properties properties) {

            String pdName = properties.getProperty(String.format(VOLUME_PROPERTY, name, getType()));
            String fsType = properties.getProperty(String.format(VOLUME_PROPERTY, name, VOLUME_GCE_FS_TYPE));
            Integer partition = toInt(properties.getProperty(String.format(VOLUME_PROPERTY, name, VOLUME_GCE_FS_TYPE)));
            Boolean readOnly = toBool(properties.getProperty(String.format(VOLUME_PROPERTY, name, READONLY)));

            return new VolumeBuilder()
                    .withName(name)
                    .withNewGcePersistentDisk(fsType, partition, pdName, readOnly)
                    .build();
        }

    }, GLUSTER_FS_PATH("glusterFsPath") {
        public  Volume fromProperties(String name, Properties properties) {
            String path = properties.getProperty(String.format(VOLUME_PROPERTY, name, getType()));
            String endpoints = properties.getProperty(String.format(VOLUME_PROPERTY, name, VOLUME_GLUSTERFS_ENDPOINTS));
            Boolean readOnly = toBool(properties.getProperty(String.format(VOLUME_PROPERTY, name, READONLY)));

            return new VolumeBuilder()
                    .withName(name)
                    .withNewGlusterfs(path, endpoints, readOnly)
                    .build();
        }

    }, PERSISTENT_VOLUME_CLAIM("persistentVolumeClaim") {
        public  Volume fromProperties(String name, Properties properties) {
            String claimRef = properties.getProperty(String.format(VOLUME_PROPERTY, name, getType()));
            Boolean readOnly = toBool(properties.getProperty(String.format(VOLUME_PROPERTY, name, READONLY)));

            return new VolumeBuilder()
                    .withName(name)
                    .withNewPersistentVolumeClaim(claimRef, readOnly)
                    .build();
        }

    };

    private final String type;

    public abstract Volume fromProperties(String name, Properties properties);

    VolumeType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }


    private static final Map<String, VolumeType> VOLUME_TYPES = new HashMap<>();

    private static final String VOLUME_PREFIX = "fabric8.volume";
    private static final String VOLUME_NAME_PREFIX = VOLUME_PREFIX + ".%s";
    public static final String VOLUME_PROPERTY = VOLUME_NAME_PREFIX + ".%s";

    private static final String VOLUME_GIT_REV = "revision";
    private static final String VOLUME_SECRET_NAME = "secret";

    private static final String VOLUME_NFS_SERVER = "nfsServer";
    private static final String VOLUME_GCE_FS_TYPE = "gceFsType";
    private static final String VOLUME_GLUSTERFS_ENDPOINTS = "endpoints";
    public static final String VOLUME_PVC_REQUEST_STORAGE = "requestStorage";

    private static final String READONLY = "readOnly";


    static {
        for (VolumeType volumeType : VolumeType.values()) {
            VOLUME_TYPES.put(volumeType.getType(), volumeType);
        }
    }

    public static final VolumeType typeFor(String type) {
        return VOLUME_TYPES.get(type);
    }

    private static Boolean toBool(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj instanceof Boolean) {
            return (Boolean) obj;
        } else if (obj instanceof String) {
            return Boolean.parseBoolean((String) obj);
        } else {
            return false;
        }
    }

    private static Integer toInt(Object obj) {
        if (obj == null) {
            return 0;
        } else if (obj instanceof Integer) {
            return (Integer) obj;
        } else if (obj instanceof String) {
            return Integer.parseInt((String) obj);
        } else {
            return 0;
        }
    }

}
