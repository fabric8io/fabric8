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
import org.apache.camel.RoutesBuilder;
import org.apache.camel.TypeConverter;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.component.properties.PropertiesParser;
import org.apache.camel.spring.SpringCamelContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Opinionated auto-configuration of the Camel context. Auto-detects Camel routes available in the Spring context and
 * exposes the key Camel utilities (like producer template, consumer template and type converter).
 */
@Configuration
public class CamelAutoConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private RoutesBuilder[] routesBuilders;

    /**
     * Spring-aware Camel context for the application. Auto-detects and loads all routes available in the Spring
     * context.
     */
    @Bean
    CamelContext camelContext() throws Exception {
        CamelContext camelContext = new SpringCamelContext(applicationContext);
        if (routesBuilders != null) {
            for (RoutesBuilder routesBuilder : routesBuilders) {
                camelContext.addRoutes(routesBuilder);
            }
        }
        return camelContext;
    }

    /**
     * Default producer template for the bootstrapped Camel context.
     */
    @Bean
    ProducerTemplate producerTemplate() throws Exception {
        return camelContext().createProducerTemplate();
    }

    /**
     * Default consumer template for the bootstrapped Camel context.
     */
    @Bean
    ConsumerTemplate consumerTemplate() throws Exception {
        return camelContext().createConsumerTemplate();
    }

    @Bean
    TypeConverter typeConverter() throws Exception {
        return camelContext().getTypeConverter();
    }

    @Bean
    PropertiesParser propertiesParser() {
        return new SpringPropertiesParser();
    }

    @Bean
    PropertiesComponent properties() {
        PropertiesComponent properties = new PropertiesComponent();
        properties.setPropertiesParser(propertiesParser());
        return properties;
    }

}