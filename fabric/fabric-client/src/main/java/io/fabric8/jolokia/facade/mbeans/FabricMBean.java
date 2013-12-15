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
package io.fabric8.jolokia.facade.mbeans;

import io.fabric8.jolokia.facade.JolokiaFabricConnector;
import io.fabric8.jolokia.facade.dto.FabricRequirementsDTO;
import io.fabric8.jolokia.facade.utils.Helpers;

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

    public String requirements(FabricRequirementsDTO requirements) {
        String json = Helpers.execToJSON(connector.getJolokiaClient(), "requirements(io.fabric8.api.FabricRequirement)", requirements);
        System.out.println(json);
        return json;
    }

    public String versions() {
        return Helpers.execToJSON(connector.getJolokiaClient(), "versions()");
    }

    public String versions(List versionsFields) {
        return Helpers.execToJSON(connector.getJolokiaClient(), "versions(java.util.List)", versionsFields);
    }

    public String fabricStatus() {
        return Helpers.execToJSON(connector.getJolokiaClient(), "fabricStatus()");
    }

    public String containers() {
        return Helpers.execToJSON(connector.getJolokiaClient(), "containers()");
    }

    public String containers(List containersFields) {
        return Helpers.execToJSON(connector.getJolokiaClient(), "containers(java.util.List)", containersFields);
    }

    public String getProfiles(String version) {
        return Helpers.execToJSON(connector.getJolokiaClient(), "getProfiles(java.lang.String)", version);
    }

    public String getProfiles(String version, List profilesFields) {
        return Helpers.execToJSON(connector.getJolokiaClient(), "getProfiles(java.lang.String,java.util.List)", version, profilesFields);
    }

    public String getProfileIds(String version) {
        return Helpers.execToJSON(connector.getJolokiaClient(), "getProfileIds(java.lang.String)", version);
    }
}
