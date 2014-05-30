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
 * <p>
 * Opinionated auto-configuration of the Camel context. Auto-detects Camel routes available in the Spring context and
 * exposes the key Camel utilities (like producer template, consumer template and type converter).
 * </p>
 * <p>
 * The most important piece of functionality provided by the Camel starter is {@code CamelContext} instance. Fabric Camel starter
 * will create {@code SpringCamelContext} for your and take care of the proper initialization and shutdown of that context. Created
 * Camel context is also registered in the Spring application context (under {@code camelContext} name), so you can access it just
 * as the any other Spring bean.
 *
 * <pre>
 * {@literal @}Configuration
 * public class MyAppConfig {
 *
 *   {@literal @}Autowired
 *   CamelContext camelContext;
 *
 *   {@literal @}Bean
 *   MyService myService() {
 *     return new DefaultMyService(camelContext);
 *   }
 *
 * }
 * </pre>
 *
 * </p>
 * <p>
 * Camel starter collects all the `RoutesBuilder` instances from the Spring context and automatically injects
 * them into the provided {@code CamelContext}. It means that creating new Camel route with the Spring Boot starter is as simple as
 * adding the {@code @Component} annotated class into your classpath:
 * </p>
 *
 * <p>
 * <pre>
 * {@literal @}Component
 * public class MyRouter extends RouteBuilder {
 *
 *  {@literal @}Override
 *    public void configure() throws Exception {
 *     from("jms:invoices").to("file:/invoices");
 *   }
 *
 * }
 * </pre>
 * </p>
 *
 * <p>
 * Or creating new route {@code RoutesBuilder} in your {@code @Configuration} class:
 * </p>
 * <p>
 * <pre>
 * {@literal @}Configuration
 * public class MyRouterConfiguration {
 *
 *   {@literal @}Bean
 *   RoutesBuilder myRouter() {
 *     return new RouteBuilder() {
 *
 *       {@literal @}Override
 *       public void configure() throws Exception {
 *         from("jms:invoices").to("file:/invoices");
 *       }
 *
 *     };
 *   }
 *
 * }
 * </pre>
 * </p>
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