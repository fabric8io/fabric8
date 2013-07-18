/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.fusesource.fabric.jolokia.facade.mbeans;

import org.fusesource.fabric.jolokia.facade.utils.Helpers;
import org.fusesource.fabric.jolokia.facade.JolokiaFabricConnector;
import org.json.simple.JSONObject;

/**
 * this facade simply maps all operations and attributes of the
 * Fabric JMX MBean
 *
 * Author: lhein
 */
public class FabricMBean {

    private JolokiaFabricConnector connector;

    public FabricMBean(JolokiaFabricConnector connector) {
        this.connector = connector;
    }

    public JSONObject getFabricFields() {
        return Helpers.read(connector.getJolokiaClient(), null);
    }

    public JSONObject requirements() {
        return Helpers.exec(connector.getJolokiaClient(), "requirements()");
    }
}
