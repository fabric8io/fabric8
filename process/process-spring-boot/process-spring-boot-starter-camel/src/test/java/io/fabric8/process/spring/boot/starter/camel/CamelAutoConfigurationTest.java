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
package io.fabric8.process.spring.boot.starter.camel;

import io.fabric8.process.spring.boot.container.FabricSpringApplication;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.TypeConverter;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import static io.fabric8.process.spring.boot.container.ComponentScanningApplicationContextInitializer.BASE_PACKAGE_PROPERTY_KEY;
import static io.fabric8.process.spring.boot.starter.camel.TestRoutesConfiguration.ROUTE_ID;

public class CamelAutoConfigurationTest extends Assert {

    @Test
    public void shouldCreateCamelContext() {
        // When
        ApplicationContext applicationContext = new FabricSpringApplication().run();
        CamelContext camelContext = applicationContext.getBean(CamelContext.class);

        // Then
        assertNotNull(camelContext);
    }

    @Test
    public void shouldDetectRoutes() {
        // Given
        System.setProperty(BASE_PACKAGE_PROPERTY_KEY, "io.fabric8.process.spring.boot.starter.camel");

        // When
        ApplicationContext applicationContext = new FabricSpringApplication().run();
        CamelContext camelContext = applicationContext.getBean(CamelContext.class);
        Route route = camelContext.getRoute(ROUTE_ID);

        // Then
        assertNotNull(route);
    }

    @Test
    public void shouldLoadProducerTemplate() {
        // When
        ApplicationContext applicationContext = new FabricSpringApplication().run();
        ProducerTemplate producerTemplate = applicationContext.getBean(ProducerTemplate.class);

        // Then
        assertNotNull(producerTemplate);
    }

    @Test
    public void shouldLoadConsumerTemplate() {
        // When
        ApplicationContext applicationContext = new FabricSpringApplication().run();
        ConsumerTemplate consumerTemplate = applicationContext.getBean(ConsumerTemplate.class);

        // Then
        assertNotNull(consumerTemplate);
    }

    @Test
    public void shouldSendAndReceiveMessageWithTemplates() {
        // Given
        String message = "message";
        String seda = "seda:test";
        ApplicationContext applicationContext = new FabricSpringApplication().run();
        ProducerTemplate producerTemplate = applicationContext.getBean(ProducerTemplate.class);
        ConsumerTemplate consumerTemplate = applicationContext.getBean(ConsumerTemplate.class);

        // When
        producerTemplate.sendBody(seda, message);
        String receivedBody = consumerTemplate.receiveBody(seda, String.class);

        // Then
        assertEquals(message, receivedBody);
    }

    @Test
    public void shouldLoadTypeConverters() {
        // Given
        Long hundred = 100L;
        ApplicationContext applicationContext = new FabricSpringApplication().run();
        TypeConverter typeConverter = applicationContext.getBean(TypeConverter.class);

        // When
        Long convertedLong = typeConverter.convertTo(Long.class, hundred.toString());

        // Then
        assertEquals(hundred, convertedLong);
    }

}