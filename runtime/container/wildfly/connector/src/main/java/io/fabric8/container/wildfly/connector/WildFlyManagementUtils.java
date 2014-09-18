/*
 * #%L
 * Fabric8 :: Container :: WildFly :: Connector
 * %%
 * Copyright (C) 2014 Red Hat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package io.fabric8.container.wildfly.connector;


import io.fabric8.api.mxbean.ManagementUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.remote.JMXConnector;

/**
 * A set of management utils
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Apr-2014
 */
public final class WildFlyManagementUtils {

    // Hide ctor
    private WildFlyManagementUtils() {
    }

    public static JMXConnector getJMXConnector(String jmxServiceURL, Map<String, Object> env, long timeout, TimeUnit unit) {
        String classLoaderKey = "jmx.remote.protocol.provider.class.loader";
        if (env.get(classLoaderKey) == null) {
            ClassLoader classLoader = WildFlyManagementUtils.class.getClassLoader();
            env.put(classLoaderKey, classLoader);
        }
        return ManagementUtils.getJMXConnector(jmxServiceURL, env, timeout, unit);
    }
}
