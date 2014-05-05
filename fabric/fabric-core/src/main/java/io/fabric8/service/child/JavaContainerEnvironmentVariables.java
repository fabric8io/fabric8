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
package io.fabric8.service.child;

/**
 * Environment variables for the Java Container
 */
public class JavaContainerEnvironmentVariables {
    /**
     * The java command line agent options
     */
    public static final String FABRIC8_JAVA_AGENT = "FABRIC8_JAVA_AGENT";

    /**
     * The java command line JVM arguments
     */
    public static final String FABRIC8_JVM_ARGS = "FABRIC8_JVM_ARGS";

    /**
     * The name of the main Java class to execute
     */
    public static final String FABRIC8_JAVA_MAIN = "FABRIC8_JAVA_MAIN";

    /**
     * The command line arguments to the Java main
     */
    public static final String FABRIC8_MAIN_ARGS = "FABRIC8_MAIN_ARGS";

    public static final String[] ALL_ENV_VARS = {
            FABRIC8_JAVA_AGENT, FABRIC8_JVM_ARGS, FABRIC8_JAVA_MAIN, FABRIC8_MAIN_ARGS
    };
}
