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
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;

import static java.util.UUID.randomUUID;

@RunWith(SpringJUnit4ClassRunner.class)
@EnableAutoConfiguration
@SpringApplicationConfiguration(classes = CamelAutoConfigurationPropertiesTest.class)
public class CamelAutoConfigurationPropertiesTest extends Assert {

    // Spring context fixtures

    @Value("${from}")
    String from;

    @Bean
    RouteBuilder routeBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(from).to("{{to}}");
            }
        };
    }

    // Collaborators fixtures

    @Autowired
    CamelContext camelContext;

    @Autowired
    ProducerTemplate producerTemplate;

    // Tests

    @Test
    public void shouldResolveBothCamelAndSpringPlaceholders() throws InterruptedException {
        // Given
        MockEndpoint mockEndpoint = camelContext.getEndpoint("mock:test", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        // When
        producerTemplate.sendBody(from, "msg");

        // Then
        mockEndpoint.assertIsSatisfied();
    }

}