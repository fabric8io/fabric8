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
 * Executable Java class to be used as a base for the Fabric-managed Spring Boot applications. Its main purpose is to
 * eliminate the custom code bootstrapping the application, so end-users could create Spring Boot managed process via
 * Fabric without any custom wiring.
 */
public class FabricSpringApplication {

    // Constants

    public static final String WEB_PROPERTY_KEY = "io.fabric8.process.spring.boot.container.web";

    public static final String[] NO_ARGUMENTS = new String[0];

    // DSL state

    private ConfigurableApplicationContext parent;

    private Boolean web;

    // Context factory method

    public ConfigurableApplicationContext run(String... args) {
        SpringApplicationBuilder applicationBuilder = new SpringApplicationBuilder().
                sources(FabricSpringApplicationConfiguration.class);
        if(parent != null) {
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
        if(webSystemProperty != null) {
            applicationBuilder.web(parseBoolean(webSystemProperty));
        } else if(web != null) {
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