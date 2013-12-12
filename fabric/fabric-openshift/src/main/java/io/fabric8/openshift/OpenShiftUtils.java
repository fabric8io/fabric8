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
package io.fabric8.openshift;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.OpenShiftConnectionFactory;

import org.fusesource.common.util.Maps;
import io.fabric8.api.Container;
import io.fabric8.api.CreateContainerMetadata;

/**
 */
public class OpenShiftUtils {
    /**
     * Returns true if the given openshift configuration map has the
     * {@link OpenShiftConstants#PROPERTY_FABRIC_MANAGED} flag enabled
     */
    public static boolean isFabricManaged(Map<String, String> openshiftConfiguration) {
        return Maps.booleanValue(openshiftConfiguration, OpenShiftConstants.PROPERTY_FABRIC_MANAGED, false);
    }

    public static void close(IOpenShiftConnection connection) {
        if (connection != null) {
            ExecutorService executorService = connection.getExecutorService();
            if (executorService != null) {
                executorService.shutdown();
            }
        }
    }

    public static IOpenShiftConnection createConnection(CreateOpenshiftContainerOptions options) {
        if (options == null) {
            return null;
        }
        String serverUrl = options.getServerUrl();
        String login = options.getLogin();
        String password = options.getPassword();
        return createConnection(serverUrl, login, password);
    }

    public static IOpenShiftConnection createConnection(String serverUrl, String login, String password) {
        return new OpenShiftConnectionFactory().getConnection("fabric", login, password, serverUrl);
    }

    public static IOpenShiftConnection createConnection(Container container) {
        return createConnection(getCreateOptions(container));
    }

    public static CreateOpenshiftContainerOptions getCreateOptions(Container container) {
        CreateOpenshiftContainerMetadata metadata = getContainerMetadata(container);
        if (metadata == null) {
            return null;
        }
        return (CreateOpenshiftContainerOptions) metadata.getCreateOptions();
    }

    protected static CreateOpenshiftContainerMetadata getContainerMetadata(Container container) {
        CreateContainerMetadata<?> value = container.getMetadata();
        if (value instanceof CreateOpenshiftContainerMetadata) {
            return (CreateOpenshiftContainerMetadata)value;
        } else {
            return null;
        }
    }

    public static IApplication getApplication(Container container) {
        CreateOpenshiftContainerMetadata metadata = getContainerMetadata(container);
        if (metadata == null) {
            return null;
        }
        CreateOpenshiftContainerOptions options = metadata.getCreateOptions();
        IOpenShiftConnection connection = OpenShiftUtils.createConnection(options);
        String containerName = container.getId();
        IDomain domain = connection.getUser().getDomain(metadata.getDomainId());
        return domain.getApplicationByName(containerName);
    }

    public static IApplication getApplication(Container container,
                                              IOpenShiftConnection connection) {
        return getApplication(container, getContainerMetadata(container), connection);
    }

    public static IApplication getApplication(Container container, CreateOpenshiftContainerMetadata metadata,
                                              IOpenShiftConnection connection) {
        String containerName = container.getId();
        IDomain domain = connection.getUser().getDomain(metadata.getDomainId());
        return domain.getApplicationByName(containerName);
    }
}
