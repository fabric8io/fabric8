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
package io.fabric8.process.spring.boot.container.simple;

import io.fabric8.process.spring.boot.container.FabricSpringApplication;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.fabric8.process.spring.boot.container.FabricSpringApplication.SPRING_MAIN_SOURCES;

@Configuration
public class FabricSpringApplicationTest extends Assert {

    // Test beans

    @Bean
    String testScopedBean() {
        return "testScopedBean";
    }

    // Tests

    @Test
    public void shouldLoadFabricStarterConfiguration() {
        // Given
        System.setProperty(SPRING_MAIN_SOURCES, "io.fabric8.process.spring.boot.container.simple");

        // When
        ApplicationContext applicationContext = new FabricSpringApplication().run();
        TestStarterBean testStarterBean = applicationContext.getBean(TestStarterBean.class);

        // Then
        assertNotNull(testStarterBean);
    }

    @Test
    public void shouldLoadTestScopedBean() {
        // Given
        System.setProperty(SPRING_MAIN_SOURCES, "io.fabric8.process.spring.boot.container.simple");

        // When
        ApplicationContext applicationContext = new FabricSpringApplication().run();
        String testScopedBean = applicationContext.getBean(String.class);

        // Then
        assertNotNull(testScopedBean);
    }

}
