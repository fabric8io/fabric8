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

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ClassUtils;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.fabric8.process.spring.boot.registry.ZooKeeperProcessRegistry.autodetectZooKeeperProcessRegistry;

/**
 * {@code ApplicationContextInitializer} registering {@link ProcessRegistryPropertySource}.
 * <br><br>
 * After you add {@code process-spring-boot-registry} jar to its classpath, {@link ProcessRegistry} will be used to
 * resolve Spring properties.
 * <br><br>
 * <pre>
 *   {@literal @}Value("service.invoicing.url")
 *   String host;
 * </pre>
 *
 * {@link ProcessRegistry} will be attempted to be read before regular Spring property sources (for example before the
 * {@code application.properties} file). If the desired value will not be found in the process registry, Spring will fallback
 * to the other property sources.
 */
public class ProcessRegistryPropertySourceApplicationContextInitializer implements ApplicationContextInitializer {

    private static ProcessRegistry processRegistry;

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        List<ProcessRegistry> registries = newArrayList(new ClassPathProcessRegistry(), new InMemoryProcessRegistry());

        if (ClassUtils.isPresent("org.apache.curator.framework.CuratorFramework", getClass().getClassLoader())) {
            registries.add(autodetectZooKeeperProcessRegistry());
        }

        processRegistry = new CompositeProcessRegistry(registries);
        ProcessRegistryPropertySource propertySource = new ProcessRegistryPropertySource(processRegistry);
        applicationContext.getEnvironment().getPropertySources().addFirst(propertySource);
    }

    public static ProcessRegistry processRegistry() {
        return processRegistry;
    }

}