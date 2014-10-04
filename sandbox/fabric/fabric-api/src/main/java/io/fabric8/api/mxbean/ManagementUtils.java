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

package io.fabric8.api.mxbean;


import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * A set of management utils
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Apr-2014
 */
public final class ManagementUtils {

    // Hide ctor
    private ManagementUtils() {
    }

    public static JMXConnector getJMXConnector(String jmxServiceURL, Map<String, Object> env, long timeout, TimeUnit unit) {
        return getJMXConnectorInternal(jmxServiceURL, env, timeout, unit);
    }

    public static Map<String, Object> getDefaultEnvironment(String jmxServiceURL) {
        Map<String, Object> env = new HashMap<String, Object>();
        return env;
    }

    private static JMXConnector getJMXConnectorInternal(String jmxServiceURL, Map<String, Object> env, long timeout, TimeUnit unit) {

        JMXServiceURL serviceURL;
        try {
            serviceURL = new JMXServiceURL(jmxServiceURL);
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }

        Exception lastException = null;
        long now = System.currentTimeMillis();
        long end = now + unit.toMillis(timeout);
        while (now <= end) {
            try {
                return JMXConnectorFactory.connect(serviceURL, env);
            } catch (Exception ex) {
                lastException = ex;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    break;
                }
            }
            now = System.currentTimeMillis();
        }

        throw new IllegalStateException("Cannot obtain JMXConnector for: " + jmxServiceURL, lastException);
    }
}
