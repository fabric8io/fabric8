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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Process registry reading information from the properties file located under the certain location in the classpath
 * ({@code META-INF/io/fabric8/process/spring/boot/registry/registry.properties}).
 * <br><br>
 * In order to feed the managed process with the classpath registry, just include
 * {@code META-INF/io/fabric8/process/spring/boot/registry/registry.properties} file(s) in your classpath. Keep in mind
 * that your classpath can contain many jars providing the
 * {@code META-INF/io/fabric8/process/spring/boot/registry/registry.properties} file. Properties from all of those will
 * be added into the registry.
 */
public class ClassPathProcessRegistry implements ProcessRegistry {

    private static final String REGISTRY_FILE_PATH =
            "META-INF/io/fabric8/process/spring/boot/registry/registry.properties";

    private final Properties registry = new Properties();

    // Constructors

    public ClassPathProcessRegistry() {
        try {
            Enumeration<URL> registryProperties = getClass().getClassLoader().getResources(REGISTRY_FILE_PATH);
            while (registryProperties.hasMoreElements()) {
                registry.load(registryProperties.nextElement().openStream());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Implementations

    @Override
    public String readProperty(String key) {
        return registry.getProperty(key);
    }

}
