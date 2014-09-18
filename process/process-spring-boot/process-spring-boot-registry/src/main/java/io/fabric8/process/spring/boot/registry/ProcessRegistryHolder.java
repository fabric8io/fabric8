/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.process.spring.boot.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * If for some reasons you can't inject `ProcessRegistry` into your Spring managed beans, you can access global registry
 * instance initialized per Spring Boot JVM using static {@code ProcessRegistryHolder#processRegistry()} method.
 * <br><br>
 * <pre>
 * ProcessRegistry processRegistry = ProcessRegistryHolder.processRegistry();
 * String invoicingServiceUrl = processRegistry.readProperty("service.invoicing.url");
 * Invoice invoice = new RestTemplate().getForObject(invoicingServiceUrl + "/" + 1, Invoice.class);
 * </pre>
 */
public class ProcessRegistryHolder {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessRegistryHolder.class);

    private static ProcessRegistry processRegistry;

    static void processRegistry(ProcessRegistry registry) {
        LOG.debug("Setting global process registry: {}", registry);
        processRegistry = registry;
    }

    public static ProcessRegistry processRegistry() {
        return processRegistry;
    }

}
