/**
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
package io.fabric8.support.api;

import java.util.List;

/**
 * {@link Collector}s are used by the {@link io.fabric8.support.api.SupportService} to collect
 * information for the support ZIP file.
 */
public interface Collector {

    /**
     * Collect a set of resources to be included in the support information ZIP file.
     *
     * @param factory a convenient factory for a few commonly used resource types
     * @return the list of resources
     */
    List<Resource> collect(ResourceFactory factory);

}
