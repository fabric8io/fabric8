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
public interface CreateContainerOptions extends Serializable {

    public String getName();
    public void setName(String name);

    public String getParent();
    public void setParent(String parent);

    public String getProviderType();
    public void setProviderType(String providerType);

    public URI getProviderURI();
    public void setProviderURI(URI providerURI);

    public boolean isEnsembleServer();
    public void setEnsembleServer(boolean setEnsembleServer);

    public String getResolver();
    public void setResolver(String resolver);

    public Map<String,Properties> getSystemProperties();

    public String getPreferredAddress();
    public void setPreferredAddress(String preferredAddress);

    public Integer getNumber();
    public void setNumber(Integer number);

    public URI getProxyUri();
    public void setProxyUri(URI proxyUri);

    public String getZookeeperUrl();
    public void setZookeeperUrl(String zookeeperUrl);

    public String getJvmOpts();
    public void setJvmOpts(String jvmOpts);
}
