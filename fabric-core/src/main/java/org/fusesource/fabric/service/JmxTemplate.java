/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.service;

import org.fusesource.fabric.api.FabricException;
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

    public <T> T execute(JmxConnectorCallback<T> callback) {
        JMXConnector connector = getConnector();
        try {
            return callback.doWithJmxConnector(getConnector());
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            try {
                close();
            } catch (Exception e2) {
                LOGGER.debug("Exception when attempting to close connection " + e2 + " after getting exception: " + e, e2);
            }
            throw new FabricException(e);
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
