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
package io.fabric8.container.process;

import io.fabric8.common.util.Strings;
import io.fabric8.service.child.ChildConstants;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;

import java.util.Map;

import static io.fabric8.service.child.JavaContainerEnvironmentVariables.FABRIC8_JAVA_AGENT;
import static io.fabric8.service.child.JavaContainerEnvironmentVariables.FABRIC8_JAVA_MAIN;
import static io.fabric8.service.child.JavaContainerEnvironmentVariables.FABRIC8_JVM_ARGS;
import static io.fabric8.service.child.JavaContainerEnvironmentVariables.FABRIC8_MAIN_ARGS;

/**
 * Represents the configuration for a Java Container when used with a child or docker container
 */
@Component(name = "io.fabric8.container.java", label = "Fabric8 Java Child Container Configuration", immediate = false, metatype = true)
public class JavaContainerConfig {

    @Property(label = "Jar URL", cardinality = 1,
            description = "The URL (usually using maven coordinates) for the jar to install.")
    private String jarUrl;

    @Property(label = "Java main class",
            description = "The name of the Java class which contains a static main(String[] args) function.")
    private String mainClass;

    @Property(label = "Arguments",
            description = "The Java main's command line arguments to pass in.")
    private String arguments;

    @Property(label = "Java Agent",
            description = "The JVM's Java Agent setting. Usually we enable jolokia for this value.")
    private String javaAgent;

    @Property(label = "JVM arguments",
            description = "The JVM command line options such as to set the memory size and garbage collection settings.")
    private String jvmArguments;

    @Property(name = "overlayFolder", label = "Overlay folder path", value = "overlayFiles",
            description = "The folder path inside the profile used to contain files and MVEL templates which are then overlayed ontop of the process installation; for customizing the configuration of the process with configuration files maintained inside the profile; possibly with dynamically resolved values.")
    private String overlayFolder;

    public void updateEnvironmentVariables(Map<String, String> environmentVariables, boolean isJavaContainer) {
        if (Strings.isNotBlank(mainClass)) {
            environmentVariables.put(FABRIC8_JAVA_MAIN, mainClass);
        } else {
            if (isJavaContainer) {
                throw new IllegalArgumentException("No mainClass value is specified in the " + ChildConstants.JAVA_CONTAINER_PID + " configuration!");
            } else {
                environmentVariables.remove(FABRIC8_JAVA_MAIN);
            }
        }
        if (Strings.isNotBlank(arguments)) {
            environmentVariables.put(FABRIC8_MAIN_ARGS, arguments);
        }
        if (Strings.isNotBlank(javaAgent)) {
            environmentVariables.put(FABRIC8_JAVA_AGENT, javaAgent);
        }
        if (Strings.isNotBlank(jvmArguments)) {
            environmentVariables.put(FABRIC8_JVM_ARGS, jvmArguments);
        }
    }

    public String getJarUrl() {
        return jarUrl;
    }

    public void setJarUrl(String jarUrl) {
        this.jarUrl = jarUrl;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    public String getJavaAgent() {
        return javaAgent;
    }

    public void setJavaAgent(String javaAgent) {
        this.javaAgent = javaAgent;
    }

    public String getJvmArguments() {
        return jvmArguments;
    }

    public void setJvmArguments(String jvmArguments) {
        this.jvmArguments = jvmArguments;
    }

    public String getOverlayFolder() {
        return overlayFolder;
    }

    public void setOverlayFolder(String overlayFolder) {
        this.overlayFolder = overlayFolder;
    }
}
