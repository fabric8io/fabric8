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

import org.fusesource.fabric.jolokia.facade.JolokiaFabricConnector;
import org.fusesource.fabric.jolokia.facade.utils.Helpers;
import org.json.simple.JSONObject;

import java.util.List;

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

    public String getFabricFields() {
        return Helpers.readToJSON(connector.getJolokiaClient(), null);
    }

    public String requirements() {
        return Helpers.execToJSON(connector.getJolokiaClient(), "requirements()");
    }

    public String requirements(Object requirements) {
        return Helpers.execToJSON(connector.getJolokiaClient(), "requirements()", requirements);
    }

    public String versions() {
        return Helpers.execToJSON(connector.getJolokiaClient(), "versions()");
    }

    public String versions(List versions) {
        return Helpers.execToJSON(connector.getJolokiaClient(), "versions(java.util.List)", versions);
    }
}
