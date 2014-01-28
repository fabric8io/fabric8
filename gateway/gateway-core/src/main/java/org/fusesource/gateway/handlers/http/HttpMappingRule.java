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
 */
public interface HttpMappingRule {
    /**
     * Using the mapping rules add all the mapped services to the given map with the URI prefix as the key of the map and
     * the mapped services as the value
     */
    void appendMappedServices(Map<String, MappedServices> uriToServicesMap);

    void addChangeListener(Runnable listener);

    void removeChangeListener(Runnable listener);
}
