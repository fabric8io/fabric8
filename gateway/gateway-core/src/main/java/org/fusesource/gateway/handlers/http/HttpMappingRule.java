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

import org.fusesource.gateway.ServiceDetails;

import java.util.List;
import java.util.Map;

/**
 * Represents a HTTP mapping rule base that can be bound to service discovery mechanisms like ZooKeeper.
 */
public interface HttpMappingRule {
    /**
     * Using the mapping rules add all the mapped services to the given map with the URI prefix as the key of the map and
     * the mapped services as the value
     */
    void appendMappedServices(Map<String, MappedServices> uriToServicesMap);

    void addChangeListener(Runnable listener);

    void removeChangeListener(Runnable listener);

    /**
     * Updates the mapping rule with details of the service URLs and parameters at the given path
     *
     * @param remove whether to remove (if true) or add (if false) this mapping
     * @param path the path that this mapping is bound
     * @param services the HTTP URLs of the services to map to
     * @param defaultParams the default parameters to use in the URI templates such as for version and container
     * @param serviceDetails
     */
    void updateMappingRules(boolean remove, String path, List<String> services, Map<String, String> defaultParams, ServiceDetails serviceDetails);
}
