/**
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
package org.fusesource.fabric.api;

import org.fusesource.fabric.api.data.BundleInfo;
import org.fusesource.fabric.api.data.ServiceInfo;
import org.fusesource.fabric.service.AgentTemplate;

import java.util.List;

public interface Agent {

    String getType();

    String getId();

    Agent getParent();

    boolean isAlive();

    // Runtime informations
    boolean isRoot();
    String getSshUrl();
    String getJmxUrl();

    Version getVersion();
    void setVersion(Version version);

    Profile[] getProfiles();
    void setProfiles(Profile[] profiles);

    String getLocation();
    void setLocation(String location);

    void start();
    void stop();
    void destroy();

    //  gets children agents, eg process instances, maybe camel contexts
    Agent[] getChildren();

    List<String> getJmxDomains();

    @Deprecated()
    BundleInfo[] getBundles();
    @Deprecated
    ServiceInfo[] getServices();

    BundleInfo[] getBundles(AgentTemplate template);
    ServiceInfo[] getServices(AgentTemplate template);

    /**
     * Returns true if the initial provisioning of the agent is complete so that we can connect to it
     * via SSH / JMX etc (e.g. the ZK ensemble is joined and the security realm is in place).
     */
    boolean isProvisioningComplete();

    String getProvisionResult();
    String getProvisionException();
    
    String getProvisionStatus();
}
