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
package org.fusesource.fabric.openshift;

import org.fusesource.common.util.Maps;
import org.fusesource.common.util.Strings;
import org.fusesource.common.util.Systems;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.ContainerAutoScaler;
import org.fusesource.fabric.api.Containers;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.NameValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 */
public class OpenShiftAutoScaler implements ContainerAutoScaler {
    private static final transient Logger LOG = LoggerFactory.getLogger(OpenShiftAutoScaler.class);

    private final OpenshiftContainerProvider containerProvider;

    public OpenShiftAutoScaler(OpenshiftContainerProvider containerProvider) {
        this.containerProvider = containerProvider;
    }

    @Override
    public void createContainers(String version, String profile, int count) throws Exception {
        CreateOpenshiftContainerOptions.Builder builder = createAuthoScaleOptions();
        if (builder != null) {
            // TODO this is actually generic to all providers! :)
            for (int i = 0; i < count; i++) {
                FabricService fabricService = containerProvider.getFabricService();
                Container[] containers = fabricService.getContainers();
                final CreateOpenshiftContainerOptions.Builder configuredBuilder = builder.number(1).version(version).profiles(profile);

                NameValidator nameValidator = containerProvider.createNameValidator(configuredBuilder.build());
                String name = Containers.createContainerName(containers, profile, containerProvider.getScheme(), nameValidator);

                CreateOpenshiftContainerOptions options = configuredBuilder.name(name).build();
                LOG.info("Creating container name " + name + " version " + version + " profile " + profile + " " + count + " container(s)");
                fabricService.createContainers(options);
            }
        } else {
            LOG.warn("Could not create version " + version + " profile " + profile + " due to missing autoscale configuration");
        }
    }

    protected CreateOpenshiftContainerOptions.Builder createAuthoScaleOptions() {
        CreateOpenshiftContainerOptions.Builder builder = CreateOpenshiftContainerOptions.builder();

        Map<String, ?> properties = containerProvider.getConfiguration();

        String serverUrl = validateProperty(properties,
                "serverUrl",
                OpenshiftContainerProvider.PROPERTY_AUTOSCALE_SERVER_URL,
                "OPENSHIFT_BROKER_HOST",
                OpenShiftConstants.DEFAULT_SERVER_URL);

        String domain = validateProperty(properties,
                "domain",
                OpenshiftContainerProvider.PROPERTY_AUTOSCALE_DOMAIN,
                "OPENSHIFT_NAMESPACE",
                "");

        String login = validateProperty(properties,
                "login",
                OpenshiftContainerProvider.PROPERTY_AUTOSCALE_LOGIN,
                "OPENSHIFT_LOGIN",
                "");

        String password = validateProperty(properties,
                "login",
                OpenshiftContainerProvider.PROPERTY_AUTOSCALE_PASSWORD,
                "OPENSHIFT_PASSWORD",
                "");

        if (Strings.isNotBlank(domain) && Strings.isNotBlank(login) && Strings.isNotBlank(password)) {
            LOG.info("Using serverUrl: " + serverUrl + " domain: " + domain + " login: " + login);
            return builder.serverUrl(serverUrl).domain(domain).login(login).password(password);
        } else {
            return null;
        }
    }


    protected String validateProperty(Map<String, ?> properties, String name, String propertyName, String envVarName, String defaultValue) {
        String answer = Maps.stringValue(properties, propertyName, Systems.getEnvVar(envVarName, defaultValue));
        if (Strings.isNullOrBlank(answer)) {
            LOG.warn("No configured value for " + name + " in property " + propertyName + " or environment variable $" + envVarName);
        }
        return answer;
    }

    @Override
    public void destroyContainers(String profile, int count, List<Container> containers) {
        // TODO

    }
}
