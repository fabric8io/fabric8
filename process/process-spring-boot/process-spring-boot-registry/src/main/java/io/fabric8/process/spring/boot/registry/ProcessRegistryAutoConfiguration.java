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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class ProcessRegistryAutoConfiguration {

    @Autowired(required = false)
    RegistryProperties registryProperties = new RegistryProperties(new HashMap<String, Object>());

    @ConditionalOnClass(org.junit.runner.Runner.class)
    @Bean
    ProcessRegistry inMemoryProcessRegistry() {
        return new InMemoryProcessRegistry(registryProperties.properties());
    }

    @ConditionalOnMissingClass(org.junit.runner.Runner.class)
    @Bean
    ProcessRegistry classPathProcessRegistry() {
        return new ClassPathProcessRegistry();
    }

}