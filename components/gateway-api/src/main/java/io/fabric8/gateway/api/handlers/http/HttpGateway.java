/*
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
package io.fabric8.gateway.api.handlers.http;

import io.fabric8.gateway.api.CallDetailRecord;
import io.fabric8.gateway.api.apimanager.ApiManager;
import io.fabric8.gateway.api.handlers.http.HttpMappingRule;
import io.fabric8.gateway.api.handlers.http.IMappedServices;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * An interface to working with a HTTP gateway which has the responsibility for
 * configuring a number of mapping rules so that the
 * {@link HttpGatewayHandler} can interrogate
 * the mapping rules via the {@link #getMappedServices()} method so it can decide which
 * services to proxy requests to.
 */
public interface HttpGateway {
    /**
     * Adds a mapping rule for exposing a number of services at URI prefixes
     */
    void addMappingRuleConfiguration(HttpMappingRule mappingRule);

    /**
     * Removes a mapping rule
     */
    void removeMappingRuleConfiguration(HttpMappingRule mappingRule);

    /**
     * Returns the currently mapped services indexed by URI prefix on this HTTP gateway
     */
    Map<String, IMappedServices> getMappedServices();

    /**
     * Returns the base URL the gateway address
     */
    public String getGatewayUrl();
    /**
     * Returns true if the mapping index is enabled which by default
     * returns a JSON document describing the mapping of URI prefixes to services
     */
    boolean isEnableIndex();

    /**
     * Returns address the gateway service is listening on.
     */
    public InetSocketAddress getLocalAddress();
    
    /**
     * Returns a handle to the 3d party API Manager to be used with this gateway.
     */
    public ApiManager getApiManager();
    
    /**
     * Adds a CallDetailRecord for reporting purposes
     */
    public void addCallDetailRecord(CallDetailRecord cdr);

}
