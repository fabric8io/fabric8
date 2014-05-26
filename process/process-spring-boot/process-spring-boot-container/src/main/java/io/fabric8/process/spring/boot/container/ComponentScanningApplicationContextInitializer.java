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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;

/**
 * <p>
 *     Spring project-level components scanner. Resolves base package from the system property, so end-user doesn't have to
 * create custom wiring code.
 * </p>
 * <p>
 *     This class can will be removed after migration to Spring Boot 1.1.0
 *     (see <a href="https://github.com/spring-projects/spring-boot/pull/951">Spring-Boot-951</a>).
 * </p>
 */
public class ComponentScanningApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentScanningApplicationContextInitializer.class);

    public static final String BASE_PACKAGE_PROPERTY_KEY = "spring.main.sources";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        LOG.debug("Initializing Fabric Spring Boot component scanner...");
        String basePackage = System.getProperty(BASE_PACKAGE_PROPERTY_KEY);
        if (basePackage != null) {
            LOG.debug("Found base package definition: {}={}", BASE_PACKAGE_PROPERTY_KEY, basePackage);
            ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner((BeanDefinitionRegistry) applicationContext, true);
            scanner.setResourceLoader(applicationContext);
            scanner.scan(basePackage);
        } else {
            LOG.debug("No base package definition ({}) found.", BASE_PACKAGE_PROPERTY_KEY);
        }
    }

}