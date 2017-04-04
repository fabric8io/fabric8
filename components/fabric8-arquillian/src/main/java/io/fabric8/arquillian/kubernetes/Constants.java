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

public class Constants {

    public static final String KUBERNETES_MASTER = "kubernetes.master";
    public static final String KUBERNETES_NAMESPACE = "kubernetes.namespace";
    public static final String KUBERNETES_DOMAIN = "kubernetes.domain";
    public static final String FABRIC8_ENVIRONMENT = "fabric8.environment";

    public static final String NAMESPACE_LAZY_CREATE_ENABLED = "namespace.lazy.enabled";
    public static final String NAMESPACE_CLEANUP_TIMEOUT = "namespace.cleanup.timeout";
    public static final String NAMESPACE_CLEANUP_CONFIRM_ENABLED = "namespace.cleanup.confirm.enabled";
    public static final String NAMESPACE_DELETE_ALL_RESOURCES_ON_EXIT = "namespace.delete.all.resources.on.exit";
    public static final String NAMESPACE_CLEANUP_ENABLED = "namespace.cleanup.enabled";
    public static final String NAMESPACE_TO_USE = "namespace.use.existing";
    public static final String DEVELOPMENT_NAMESPACE = "development.namespace";
    public static final String FAIL_ON_MISSING_ENVIRONMENT_NAMESPACE = "fail.on.missing.environment.namespace";
    public static final String CREATE_NAMESPACE_FOR_TEST = "create.namespace.for.test";

    public static final String ENVIRONMENT_INIT_ENABLED = "env.init.enabled";
    public static final String ENVIRONMENT_CONFIG_URL = "env.config.url";
    public static final String ENVIRONMENT_CONFIG_RESOURCE_NAME = "env.config.resource.name";
    public static final String ENVIRONMENT_DEPENDENCIES = "env.dependencies";

    public static final String WAIT_TIMEOUT = "wait.timeout";
    public static final String WAIT_POLL_INTERVAL = "wait.poll.interval";

    public static final String WAIT_FOR_SERVICE_LIST = "wait.for.service.list";
    public static final String WAIT_FOR_SERVICE_CONNECTION_ENABLED = "wait.for.service.connection.enabled";
    public static final String WAIT_FOR_SERVICE_CONNECTION_TIMEOUT = "wait.for.service.connection.timeout";

    public static final String ANSI_LOGGER_ENABLED = "ansi.logger.enabled";
    public static final String GOFABRIC8_ENABLED = "gofabric8.enabled";
    public static final String CLIENT_CREATOR_CLASS_NAME = "kubernetes.client.creator.class.name";
    public static final String KUBERNETES_MODEL_PROCESSOR_CLASS = "kubernetes.model.processor.class.name";

    // Non-config constants
    public static final String JAVA_PROTOCOL_HANDLER = "java.protocol.handler.pkgs";
    public static final String PROTOCOL_HANDLERS = "protocolHandlers";
    public static final String DEFAULT_MAVEN_PROTOCOL_HANDLER = "org.ops4j.pax.url";

    //Defaults
    public static final String DEFAULT_CONFIG_FILE_NAME = "kubernetes.json";
    public static final Long DEFAULT_NAMESPACE_CLEANUP_TIMEOUT = 0L;
    public static final Boolean DEFAULT_NAMESPACE_CLEANUP_ENABLED = true;
    public static final Boolean DEFAULT_NAMESPACE_LAZY_CREATE_ENABLED = true;

    /**
     * We often won't be able to connect to the services from the JUnit test case
     * unless the user explicitly knows its OK and allows it. (e.g. there may not be a network route)
     */
    public static final Boolean DEFAULT_WAIT_FOR_SERVICE_CONNECTION_ENABLED = false;
    public static final Long DEFAULT_WAIT_FOR_SERVICE_CONNECTION_TIMEOUT = 10 * 1000L;
    public static final Long DEFAULT_WAIT_TIMEOUT = 5 * 60 * 1000L;
    public static final Long DEFAULT_WAIT_POLL_INTERVAL = 5 * 1000L;

    public static final String DEFAULT_KUBERNETES_MASTER = "https://kubernetes.default.svc";

    public static final String RUNNING_STATUS = "RUNNING";
    public static final String ABORTED_STATUS = "ABORTED";
    public static final String ERROR_STATUS = "ERROR";

    public static final String FABRIC8_DOCKER_REGISTRY_SERVICE_HOST = "FABRIC8_DOCKER_REGISTRY_SERVICE_HOST";
    public static final String FABRIC8_DOCKER_REGISTRY_SERVICE_PORT = "FABRIC8_DOCKER_REGISTRY_SERVICE_PORT";

}
