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
package io.fabric8.runtime.container.wildfly;

import java.io.IOException;
import java.util.Properties;

import io.fabric8.runtime.container.ContainerConfiguration;

import org.jboss.gravia.repository.MavenCoordinates;
import org.jboss.gravia.runtime.RuntimeType;


/**
 * The managed container configuration
 *
 * @since 26-Feb-2014
 */
public final class WildFlyContainerConfiguration extends ContainerConfiguration {

    public static final String DEFAULT_JAVAVM_ARGUMENTS = "-Xmx1024m";

    private String serverConfig = "standalone-fabric.xml";

    @Override
    public RuntimeType getRuntimeType() {
        return RuntimeType.WILDFLY;
    }

    @Override
    protected void validate() {
        if (getMavenCoordinates().isEmpty()) {
            Properties properties = new Properties();
            try {
                properties.load(getClass().getResourceAsStream("version.properties"));
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot load version.properties", ex);
            }
            String wildflyVersion = properties.getProperty("wildfly.version");
            String projectVersion = properties.getProperty("project.version");
            addMavenCoordinates(MavenCoordinates.create("org.wildfly", "wildfly-dist", wildflyVersion, "zip", null));
            addMavenCoordinates(MavenCoordinates.create("io.fabric8.runtime", "fabric-runtime-container-wildfly-patch", projectVersion, "zip", null));
        }
        super.validate();
    }

    public String getServerConfig() {
        return serverConfig;
    }

}
