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
package io.fabric8.service;

import java.io.IOException;
import javax.management.remote.JMXConnector;

import io.fabric8.api.FabricException;

/**
 * This implementation closes the connector down after each operation; so only really intended for web applications.
 *
 * @author ldywicki
 */
public abstract class NonCachingJmxTemplate extends JmxTemplateSupport {

    public <T> T execute(JmxConnectorCallback<T> callback) {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            JMXConnector connector = createConnector();
            if (connector == null) {
                throw new IllegalStateException("JMX connector can not be created");
            }
            try {
                return callback.doWithJmxConnector(connector);
            } catch (Exception e) {
                throw FabricException.launderThrowable(e);
            } finally {
                try {
                    connector.close();
                } catch (IOException e) {
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    protected abstract JMXConnector createConnector();

}
