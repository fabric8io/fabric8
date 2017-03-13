/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.arquillian.kubernetes;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Strings;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static io.fabric8.arquillian.kubernetes.Constants.DEFAULT_MAVEN_PROTOCOL_HANDLER;
import static io.fabric8.arquillian.kubernetes.Constants.PROTOCOL_HANDLERS;
import static io.fabric8.arquillian.kubernetes.Constants.JAVA_PROTOCOL_HANDLER;

public class Configurer {

    private static final String EXTENSION_NAME = "kubernetes";

    @Inject
    @ApplicationScoped
    private InstanceProducer<Configuration> configurationProducer;

    public void configure(@Observes ArquillianDescriptor arquillianDescriptor) {
        Map<String, String> config = arquillianDescriptor.extension(EXTENSION_NAME).getExtensionProperties();
        configurationProducer.set(Configuration.fromMap(config, null));
        configureProtocolHandlers(config);
    }
    
    private static void configureProtocolHandlers(Map<String, String> conf) {
        Set<String> handlers = new LinkedHashSet<>();
        handlers.addAll(Strings.splitAndTrimAsList(System.getProperty(JAVA_PROTOCOL_HANDLER, ""), " "));
        handlers.addAll(Strings.splitAndTrimAsList(conf.containsKey(PROTOCOL_HANDLERS) ? conf.get(PROTOCOL_HANDLERS) : DEFAULT_MAVEN_PROTOCOL_HANDLER, " "));
        System.setProperty(JAVA_PROTOCOL_HANDLER, Strings.join(handlers, " "));
    }
}
