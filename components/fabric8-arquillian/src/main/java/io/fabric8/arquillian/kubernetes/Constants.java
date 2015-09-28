/**
 *  Copyright 2005-2015 Red Hat, Inc.
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

public class Constants {

    public static final String KUBERNETES_MASTER="KUBERNETES_MASTER";
    public static final String KUBERNETES_NAMESPACE="KUBERNETES_NAMESPACE";
    public static final String KUBERNETES_DOMAIN="KUBERNETES_DOMAIN";

    public static final String FABRIC8_NAMESPACE_DESTROY_TIMEOUT = "FABRIC8_NAMESPACE_DESTROY_TIMEOUT";
    public static final String FABRIC8_NAMESPACE_CONFIRM_DESTROY = "FABRIC8_NAMESPACE_CONFIRM_DESTROY";

    public static final String MASTER_URL = "masterUrl";
    public static final String CONFIG_URL = "configUrl";
    public static final String DEPENDENCIES = "dependencies";
    public static final String JAVA_PROTOCOL_HANDLER = "java.protocol.handler.pkgs";
    public static final String PROTOCOL_HANDLERS = "protocolHandlers";
    public static final String DEFAULT_MAVEN_PROTOCOL_HANDLER = "org.ops4j.pax.url";

    public static final String WAIT_FOR_SERVICES = "waitForServices";
    public static final String CONNECT_TO_SERVICES = "connectToServices";
    public static final String WAIT_FOR_SERVICE_CONNECTION = "waitForServiceConnection";
    public static final String SERVICE_CONNECTION_TIMEOUT = "serviceConnectionTimeout";

    public static final String USE_EXISTING_NAMESPACE = "useExistingNamespace";
    public static final String LAZY_CREATE_NAMESPACE = "lazyCreateNamespace";
    public static final String NAMESPACE_CLEANUP_ENABLED = "namespaceCleanup";

    public static final String TIMEOUT = "timeout";
    public static final String POLL_INTERVAL = "pollInterval";

    public static final String CONFIG_FILE_NAME = "configFileName";
    public static final String DEFAULT_CONFIG_FILE_NAME = "kubernetes.json";

    public static final String ANSI_LOGGER_ENABLED = "ansi.logger.enabled";
}
