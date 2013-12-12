/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.api;

import java.io.Serializable;
import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 */
public interface CreateContainerOptions extends Serializable, Cloneable {

    /**
     * Creates a new instance of {@link CreateContainerOptions} with updated credentials.
     * @param user
     * @param credential
     * @return
     */
    CreateContainerOptions updateCredentials(String user, String credential);

    String getName();

    String getParent();

    String getProviderType();

    boolean isEnsembleServer();

    String getResolver();

    String getManualIp();

    Map<String, Properties> getSystemProperties();

    String getPreferredAddress();

    String getBindAddress();

    int getMinimumPort();

    int getMaximumPort();

    Integer getNumber();

    URI getProxyUri();

    String getZookeeperUrl();

    String getZookeeperPassword();

    String getJvmOpts();


    boolean isAdminAccess();

    Map<String, ? extends CreateContainerMetadata> getMetadataMap();

    Set<String> getProfiles();

    String getVersion();

}
