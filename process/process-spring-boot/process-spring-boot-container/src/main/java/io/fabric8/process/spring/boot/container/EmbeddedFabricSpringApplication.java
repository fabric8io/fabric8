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
package io.fabric8.process.spring.boot.container;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * <p>
 * Bean that can be added to the existing Spring application context in order to start embedded Fabric Spring Boot
 * context from within it. The embedding context (the one {@link EmbeddedFabricSpringApplication} has been added to)
 * will become a parent context for the embedded Fabric Spring Boot context.
 * </p>
 * <p>
 * Creating embedded Fabric application is as simple as that:
 * <pre>
 *     @Bean
 *     EmbeddedFabricSpringApplication fabricSpringApplication() {
 *         return new EmbeddedFabricSpringApplication();
 *     }
 * </pre>
 * </p>
 */
public class EmbeddedFabricSpringApplication implements ApplicationContextAware {

    // Members

    /**
     * @see #context()
     */
    private ConfigurableApplicationContext context;

    // Lifecycle callbacks

    @Override
    public void setApplicationContext(ApplicationContext parent) throws BeansException {
        context = new FabricSpringApplication().parent((ConfigurableApplicationContext) parent).run();
    }

    // Accessors

    /**
     * Embedded Fabric Spring Boot application context.
     *
     * @return embedded Spring Boot context
     */
    public ConfigurableApplicationContext context() {
        return context;
    }

}