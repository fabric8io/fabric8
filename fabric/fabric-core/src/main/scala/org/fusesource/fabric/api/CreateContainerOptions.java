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
package org.fusesource.fabric.api;

import java.io.Serializable;
import java.net.URI;
import java.util.Map;
import java.util.Properties;

/**
 */
public interface CreateContainerOptions extends Serializable, Cloneable {

    String getName();
    void setName(String name);

    String getParent();
    void setParent(String parent);

    String getProviderType();
    void setProviderType(String providerType);

    boolean isEnsembleServer();
    void setEnsembleServer(boolean setEnsembleServer);

    String getResolver();
    void setResolver(String resolver);

    Map<String,Properties> getSystemProperties();

    String getPreferredAddress();
    void setPreferredAddress(String preferredAddress);

    int getMinimumPort();
    void setMinimumPort(int port);

    int getMaximumPort();
    void setMaximumPort(int port);

    Integer getNumber();
    void setNumber(Integer number);

    URI getProxyUri();
    void setProxyUri(URI proxyUri);

    String getZookeeperUrl();
    void setZookeeperUrl(String zookeeperUrl);

    String getZookeeperPassword();
    void setZookeeperPassword(String zookeeperPassword);

    String getJvmOpts();
    void setJvmOpts(String jvmOpts);


    boolean isAdminAccess();
    void setAdminAccess(boolean adminAccess);

    void setCreationStateListener(CreationStateListener listener);
    CreationStateListener getCreationStateListener();

    Map<String, ? extends CreateContainerMetadata> getMetadataMap();

}
