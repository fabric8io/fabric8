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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In order to use the Process Registry API directly, you can inject {@code ProcessRegistry} into your Spring managed
 * beans.
 * <br><br>
 * <pre>
 * private final ProcessRegistry processRegistry;
 *
 * {@literal @}Autowired
 * public MyBean(ProcessRegistry processRegistry) {
 * this.processRegistry = processRegistry;
 * }
 *
 * ...
 *
 * String invoicingServiceUrl = processRegistry.readProperty("service.invoicing.url");
 * Invoice invoice = new RestTemplate().getForObject(invoicingServiceUrl + "/" + 1, Invoice.class);
 * </pre>
 */
@Configuration
public class ProcessRegistryAutoConfiguration {

    @Bean
    ProcessRegistry processRegistry() {
        return ProcessRegistryHolder.processRegistry();
    }

}