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
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.fabric8.process.spring.boot.registry.ZooKeeperProcessRegistry.autodetectZooKeeperProcessRegistry;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * {@code ApplicationContextInitializer} registering {@link ProcessRegistryPropertySource}.
 * <br><br>
 * After you add {@code process-spring-boot-registry} jar to the classpath of your application, {@link ProcessRegistry}
 * will be used to resolve Spring properties.
 * <br><br>
 * <pre>
 *   {@literal @}Value("service.invoicing.url")
 *   String host;
 * </pre>
 *
 * {@link ProcessRegistry} will be attempted to be read before regular Spring property sources (for example before the
 * {@code application.properties} file). If the desired value will not be found in the process registry, Spring will fallback
 * to the other property sources.
 * <br><br>
 * By default the following process registries are aggregated by the Spring Boot container:
 * <br><br>
 * <li>{@link ZooKeeperProcessRegistry} (if {@code curator-framework} is present in the classpath)</li>
 * <li>{@link ClassPathProcessRegistry}</li>
 * <li>{@link InMemoryProcessRegistry}</li>
 * <br><br>
 * The above basically means that Spring Boot container attempts to read properties from the Fabric8 ZooKeeper registry,
 * then from the system properties and finally from the files located in the classpath.
 */
public class ProcessRegistryPropertySourceApplicationContextInitializer implements ApplicationContextInitializer, Ordered {

    private static final Logger LOG = getLogger(ProcessRegistryPropertySourceApplicationContextInitializer.class);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        List<ProcessRegistry> registries = newArrayList(new InMemoryProcessRegistry(), new ClassPathProcessRegistry());

        LOG.debug("Looking for ZooKeeperProcessRegistry...");
        if (ClassUtils.isPresent("org.apache.curator.framework.CuratorFramework", getClass().getClassLoader())) {
            LOG.info("Apache Curator jar found. Creating ZooKeeperProcessRegistry.");
            registries.add(autodetectZooKeeperProcessRegistry());
        }

        ProcessRegistry registry = new CompositeProcessRegistry(registries);
        ProcessRegistryHolder.processRegistry(registry);
        ProcessRegistryPropertySource propertySource = new ProcessRegistryPropertySource(registry);
        applicationContext.getEnvironment().getPropertySources().addFirst(propertySource);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1000;
    }

}