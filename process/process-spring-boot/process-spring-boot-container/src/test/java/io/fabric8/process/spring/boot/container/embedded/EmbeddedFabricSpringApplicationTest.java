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
package io.fabric8.process.spring.boot.container.embedded;

import io.fabric8.process.spring.boot.container.EmbeddedFabricSpringApplication;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddedFabricSpringApplicationTest extends Assert {

    // Test beans

    @Bean
    String testScopedBean() {
        return "testScopedBean";
    }

    @Bean
    EmbeddedFabricSpringApplication embeddedFabricApplicationContext() {
        return new EmbeddedFabricSpringApplication();
    }

    // Tests

    @Test
    public void shouldLoadParentBean() {
        // Given
        ConfigurableApplicationContext parent = new AnnotationConfigApplicationContext(getClass());

        // When
        ApplicationContext embeddedFabricContext = parent.getBean(EmbeddedFabricSpringApplication.class).context();
        String testScopedBean = embeddedFabricContext.getBean(String.class);

        // Then
        assertNotNull(testScopedBean);
    }

}