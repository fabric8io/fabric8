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
package org.fusesource.gateway.handlers.http;

import java.util.Map;

/**
 * An interface to working with a HTTP gateway which has the responsibility for
 * configuring a number of mapping rules so that the
 * {@link org.fusesource.gateway.handlers.http.HttpGatewayHandler} can interogate
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
    Map<String, MappedServices> getMappedServices();

    /**
     * Returns true if the mapping index is enabled which by default
     * returns a JSON document describing the mapping of URI prefixes to services
     */
    boolean isEnableIndex();
}
