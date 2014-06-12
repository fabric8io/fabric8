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
package io.fabric8.process.spring.boot.container;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static java.lang.Boolean.parseBoolean;

/**
 * <p>Executable Java class to be used as a base for the Fabric-managed Spring Boot applications. Its main purpose is to
 * eliminate the custom code bootstrapping the application, so end-users could create Spring Boot managed process via
 * Fabric without any custom wiring.</p>
 * <p/>
 * <p>
 * {@link FabricSpringApplication} can be used in the conjunction with the Fabric Jar Managed Process installer (just
 * as demonstrated on the snippet below).
 * </p>
 * <p/>
 * <pre>
 *     process:install-jar -m io.fabric8.process.spring.boot.container.FabricSpringApplication my.group.id my-artifact 1.0
 * </pre>
 * <p/>
 * <p>
 * Keep in mind that you don't have to use {@link FabricSpringApplication} in order to use Fabric goodies for Spring
 * Boot (like Fabric starters). However we recommend to use this class as an entry point for your Fabric SpringBoot
 * integration, as it implements our opinionated view of the proper Fabric+Boot wiring.
 * </p>
 * <p>
 *      In order to specify packages that should be scanned for additional {@code @Component} and {@code @Configuration} classes, use
 * standard Spring Boot {@code spring.main.sources} system property. For example if your project {@code @Configuration} classes are located in
 * the {@code com.example.project} package, you can use the following command to install your jar as a managed process:
 *
 * <pre>
 *     process:install-jar -m io.fabric8.process.spring.boot.container.FabricSpringApplication --jvm-options=-Dspring.main.sources=com.example.project my.group.id my-artifact 1.0
 * </pre>
 * </p>
 */
public class FabricSpringApplication {

    // Constants

    public static final String SPRING_MAIN_SOURCES = "spring.main.sources";

    public static final String WEB_PROPERTY_KEY = "io.fabric8.process.spring.boot.container.web";

    public static final String[] NO_ARGUMENTS = new String[0];

    // DSL state

    private ConfigurableApplicationContext parent;

    private Boolean web;

    // Context factory method

    public ConfigurableApplicationContext run(String... args) {
        SpringApplicationBuilder applicationBuilder = new SpringApplicationBuilder().
                sources(FabricSpringApplicationConfiguration.class);
        if (parent != null) {
            applicationBuilder.parent(parent);
        }
        resolveWebEnvironment(applicationBuilder);
        return applicationBuilder.run(args);
    }

    // Main method

    public static void main(String[] args) {
        new FabricSpringApplication().run(args);
    }

    // Logic helpers

    protected void resolveWebEnvironment(SpringApplicationBuilder applicationBuilder) {
        // Check of the web system property should be performed by the Spring Boot - we should issue PR for this.
        String webSystemProperty = System.getProperty(WEB_PROPERTY_KEY);
        if (webSystemProperty != null) {
            applicationBuilder.web(parseBoolean(webSystemProperty));
        } else if (web != null) {
            applicationBuilder.web(web);
        }
    }

    // DSL setters

    public FabricSpringApplication parent(ConfigurableApplicationContext parent) {
        this.parent = parent;
        return this;
    }

    public FabricSpringApplication web(boolean web) {
        this.web = web;
        return this;
    }

}