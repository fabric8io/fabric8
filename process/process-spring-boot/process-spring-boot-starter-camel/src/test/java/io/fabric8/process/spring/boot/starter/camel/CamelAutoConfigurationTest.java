/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.process.spring.boot.starter.camel;

import io.fabric8.process.spring.boot.container.FabricSpringApplication;
import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import static io.fabric8.process.spring.boot.container.ComponentScanningApplicationContextInitializer.BASE_PACKAGE_PROPERTY_KEY;
import static io.fabric8.process.spring.boot.starter.camel.TestRoutesConfiguration.ROUTE_ID;

public class CamelAutoConfigurationTest extends Assert {

    @Test
    public void shouldCreateCamelContext() {
        // When
        ApplicationContext applicationContext = FabricSpringApplication.run(new String[0]);
        CamelContext camelContext = applicationContext.getBean(CamelContext.class);

        // Then
        assertNotNull(camelContext);
    }

    @Test
    public void shouldDetectRoutes() {
        // Given
        System.setProperty(BASE_PACKAGE_PROPERTY_KEY, "io.fabric8.process.spring.boot.starter.camel");

        // When
        ApplicationContext applicationContext = FabricSpringApplication.run(new String[0]);
        CamelContext camelContext = applicationContext.getBean(CamelContext.class);
        Route route = camelContext.getRoute(ROUTE_ID);

        // Then
        assertNotNull(route);
    }

}