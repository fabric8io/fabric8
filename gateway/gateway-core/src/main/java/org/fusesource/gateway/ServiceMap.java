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
package org.fusesource.gateway;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains a mapping of services which is then use by the proxy to update in process
 * proxy handlers, or used to create new proxy handers
 */
public class ServiceMap {
    private ConcurrentHashMap<String, Map<String, ServiceDetails>> map = new ConcurrentHashMap<String, Map<String, ServiceDetails>>();

    /**
     * Returns a list of all the current services for the given path
     */
    public List<ServiceDetails> getServices(String path) {
        Map<String, ServiceDetails> pathMap = getPathMap(path);
        return new ArrayList<ServiceDetails>(pathMap.values());
    }

    /**
     * Returns a list of all the current paths for the services
     */
    public List<String> getPaths() {
        return new ArrayList<String>(map.keySet());
    }

    /**
     * When a service is added or updated
     */
    public void serviceUpdated(String path, ServiceDetails service) {
        // ignore services with empty services
        if (!service.getServices().isEmpty()) {
            getPathMap(path).put(service.getId(), service);
        }
    }

    /**
     * When a service is added or updated
     */
    public void serviceRemoved(String path, ServiceDetails service) {
        getPathMap(path).remove(service.getId());

        // lets update any in progress proxy handlers using this service
    }

    protected synchronized Map<String, ServiceDetails> getPathMap(String path) {
        Map<String, ServiceDetails> initial = new ConcurrentHashMap<String, ServiceDetails>();
        Map<String, ServiceDetails> answer = map.putIfAbsent(path, initial);
        if (answer == null) {
            answer = initial;
        }
        return answer;
    }
}
