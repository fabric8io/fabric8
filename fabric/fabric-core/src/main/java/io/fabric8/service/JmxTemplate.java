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

import io.fabric8.api.FabricException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.remote.JMXConnector;
import java.io.IOException;

/**
 */
public class JmxTemplate extends JmxTemplateSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(JmxTemplate.class);

    private JMXConnector connector;

    public JmxTemplate() {
    }

    public JmxTemplate(JMXConnector connector) {
        this.connector = connector;
    }

    public <T> T execute(JmxTemplateSupport.JmxConnectorCallback<T> callback) {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            JMXConnector connector = getConnector();
            if (connector == null) {
                throw new IllegalStateException("JMX connector can not be created");
            }
            try {
                return callback.doWithJmxConnector(connector);
            } catch (Exception e) {
                try {
                    close();
                } catch (Exception e2) {
                    LOGGER.debug("Exception when attempting to close connection " + e2 + " after getting exception: " + e, e2);
                }
                throw FabricException.launderThrowable(e);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    public synchronized void close() {
        if (connector != null) {
            try {
                connector.close();
            } catch (IOException e) {
                throw new FabricException("Failed to close connection: " + connector + " on " + this + ". " + e, e);
            } finally {
                connector = null;
            }
        }
    }


    protected synchronized JMXConnector getConnector() {
        if (connector == null) {
            connector = createConnector();
        }
        return connector;
    }

    protected JMXConnector createConnector() {
        throw new UnsupportedOperationException("No JMX connector has been configured!");
    }

}
