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
package io.fabric8.runtime.container;

import java.io.File;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.jboss.gravia.repository.MavenCoordinates;
import org.jboss.gravia.runtime.RuntimeType;


/**
 * The managed container configuration builder
 *
 * @since 26-Feb-2014
 */
public abstract class ContainerConfigurationBuilder {

    public static ContainerConfigurationBuilder create(RuntimeType type) {
        ServiceLoader<ContainerConfigurationBuilder> loader = ServiceLoader.load(ContainerConfigurationBuilder.class);
        Iterator<ContainerConfigurationBuilder> iterator = loader.iterator();
        while(iterator.hasNext()) {
            ContainerConfigurationBuilder configurationBuilder = iterator.next();
            ContainerConfiguration auxconfig = configurationBuilder.internalConfiguration();
            if (auxconfig.getRuntimeType() == type) {
                return configurationBuilder;
            }
        }
        throw new IllegalStateException("Cannot obtain container configuration service for: " + type);
    }

    protected abstract ContainerConfiguration internalConfiguration();

    public ContainerConfigurationBuilder addMavenCoordinates(MavenCoordinates coordinates) {
        internalConfiguration().addMavenCoordinates(coordinates);
        return this;
    }

    public ContainerConfigurationBuilder setTargetDirectory(String target) {
        internalConfiguration().setTargetDirectory(new File(target).getAbsoluteFile());
        return this;
    }

    public ContainerConfigurationBuilder setJavaVmArguments(String javaVmArguments) {
        internalConfiguration().setJavaVmArguments(javaVmArguments);
        return this;
    }

    public ContainerConfigurationBuilder setOutputToConsole(boolean outputToConsole) {
        internalConfiguration().setOutputToConsole(outputToConsole);
        return this;
    }

    public ContainerConfiguration getConfiguration() {
        ContainerConfiguration configuration = internalConfiguration();
        configuration.validate();
        configuration.makeImmutable();
        return configuration;
    }
}
