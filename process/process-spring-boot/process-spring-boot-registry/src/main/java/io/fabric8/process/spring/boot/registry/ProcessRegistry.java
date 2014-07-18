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

/**
 * provides a unified way to access information shared by the managed processes.
 * <br><br>
 * Managed Spring Boot processes do not live in a separation. Instead those usually create a graph of microservices
 * calling each other. In order to make communication between the processes easier, Fabric8 provides a unified way to
 * access information shared by the managed Spring Boot processes - {@code ProcessRegistry}.
 * <br><br>
 * In order to start working with the Fabric8 process registry, add the following dependency to your project:
 * <br><br>
 * <pre>
 * &lt;dependency&gt;
 *   &lt;groupId&gt;io.fabric8&lt;/groupId&gt;
 *   &lt;artifactId&gt;process-spring-boot-registry&lt;/artifactId&gt;
 * &lt;/dependency&gt;
 * </pre>
 * <br><br>
 * From now on, you can enjoy your Spring Boot microservice sharing information with the other microservices managed by
 * the Fabric8.
 */
public interface ProcessRegistry {

    /**
     * Reads property from the process registry.
     *
     * @param key identifies uniquely property to be resolved
     * @return value of the property or {@code null} if property is not available
     */
    String readProperty(String key);

}
