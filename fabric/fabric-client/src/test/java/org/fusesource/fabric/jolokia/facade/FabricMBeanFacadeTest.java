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
package org.fusesource.fabric.jolokia.facade;

import org.codehaus.jackson.map.type.TypeFactory;
import org.fusesource.fabric.jolokia.facade.dto.FabricDTO;
import org.fusesource.fabric.jolokia.facade.dto.FabricRequirementsDTO;
import org.fusesource.fabric.jolokia.facade.dto.VersionDTO;
import org.fusesource.fabric.jolokia.facade.mbeans.FabricMBean;
import org.fusesource.fabric.jolokia.facade.utils.Helpers;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

/**
 * Author: lhein
 */
public class FabricMBeanFacadeTest {
    String userName = System.getProperty("fabric.user", "admin");
    String password = System.getProperty("fabric.password", "admin");

    // note, make sure there's an ending /, jolokia doesn't handle HTTP 302 too well...
    String url = System.getProperty("fabric.url", "http://localhost:8181/jolokia/");


    @Before
    public void maybeEnableLogging() {
        if (Boolean.valueOf(System.getProperty("logging"))) {
            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
            System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
            System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "DEBUG");
        }
    }

    private FabricMBean getFabricMBean() {
        JolokiaFabricConnector connector = JolokiaFabricConnector.getFabricConnector(userName, password, url);
        FabricMBean mBeanFacade = new FabricMBean(connector);
        return mBeanFacade;
    }

    @Test
    public void testRequirements() {
        // this can only be run if you have a fabric running...
        Assume.assumeTrue(Boolean.valueOf(System.getProperty("hasFabric")));

        FabricMBean facade = getFabricMBean();

        String json = facade.requirements();

        try {
            FabricRequirementsDTO dto = Helpers.getObjectMapper().readValue(json, FabricRequirementsDTO.class);
            Assume.assumeNotNull(dto);
            System.out.println(dto);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetFabricFields() {
        // this can only be run if you have a fabric running...
        Assume.assumeTrue(Boolean.valueOf(System.getProperty("hasFabric")));

        FabricMBean facade = getFabricMBean();

        String json = facade.getFabricFields();

        try {
            FabricDTO dto = Helpers.getObjectMapper().readValue(json, FabricDTO.class);
            Assume.assumeNotNull(dto);
            System.out.println(dto);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testVersions() {
        // this can only be run if you have a fabric running...
        Assume.assumeTrue(Boolean.valueOf(System.getProperty("hasFabric")));

        FabricMBean facade = getFabricMBean();

        String json = facade.versions();

        try {
            Collection<VersionDTO> versions = Helpers.getObjectMapper().readValue(json, TypeFactory.defaultInstance().constructParametricType(Collection.class, VersionDTO.class));
            Assume.assumeNotNull(versions);
            for (Object version : versions) {
                System.out.println(version);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testVersionsWithOnlyIDs() {
        // this can only be run if you have a fabric running...
        Assume.assumeTrue(Boolean.valueOf(System.getProperty("hasFabric")));

        FabricMBean facade = getFabricMBean();

        String json = facade.versions(Helpers.toList("id"));

        try {
            Collection<VersionDTO> versions = Helpers.getObjectMapper().readValue(json, TypeFactory.defaultInstance().constructParametricType(Collection.class, VersionDTO.class));
            Assume.assumeNotNull(versions);
            for (Object version : versions) {
                System.out.println(version);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
