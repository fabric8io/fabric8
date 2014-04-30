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

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.TypeConverter;
import org.apache.camel.builder.RouteBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@EnableAutoConfiguration
@SpringApplicationConfiguration(classes = CamelAutoConfigurationTest.class)
public class CamelAutoConfigurationTest extends Assert {

    // Spring context fixtures

    String routeId = "testRoute";

    @Bean
    RouteBuilder routeBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:test").routeId(routeId).to("mock:test");
            }
        };
    }

    // Collaborators fixtures

    @Autowired
    CamelContext camelContext;

    @Autowired
    ProducerTemplate producerTemplate;

    @Autowired
    ConsumerTemplate consumerTemplate;

    @Autowired
    TypeConverter typeConverter;

    // Tests

    @Test
    public void shouldCreateCamelContext() {
        assertNotNull(camelContext);
    }

    @Test
    public void shouldDetectRoutes() {
        // When
        Route route = camelContext.getRoute(routeId);

        // Then
        assertNotNull(route);
    }

    @Test
    public void shouldLoadProducerTemplate() {
        assertNotNull(producerTemplate);
    }

    @Test
    public void shouldLoadConsumerTemplate() {
        assertNotNull(consumerTemplate);
    }

    @Test
    public void shouldSendAndReceiveMessageWithTemplates() {
        // Given
        String message = "message";
        String seda = "seda:test";

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

        // When
        Long convertedLong = typeConverter.convertTo(Long.class, hundred.toString());

        // Then
        assertEquals(hundred, convertedLong);
    }

}