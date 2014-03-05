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
package io.fabric8.jolokia.facade;

import org.codehaus.jackson.map.type.TypeFactory;

import io.fabric8.jolokia.facade.dto.*;
import io.fabric8.jolokia.facade.mbeans.FabricMBean;
import io.fabric8.jolokia.facade.utils.Helpers;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collection;

/**
 * Author: lhein
 */
@Ignore("[FABRIC-791][7.4] Fix jolokia FabricMBeanFacadeTest")
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

    @Test
    public void testFabricStatus() {
        // this can only be run if you have a fabric running...
        Assume.assumeTrue(Boolean.valueOf(System.getProperty("hasFabric")));

        FabricMBean facade = getFabricMBean();

        String json = facade.fabricStatus();

        try {
            FabricStatusDTO status = Helpers.getObjectMapper().readValue(json, FabricStatusDTO.class);
            Assume.assumeNotNull(status);
            System.out.println(status);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testContainers() {
        // this can only be run if you have a fabric running...
        Assume.assumeTrue(Boolean.valueOf(System.getProperty("hasFabric")));

        FabricMBean facade = getFabricMBean();

        String json = facade.containers();

        try {
            Collection<ContainerDTO> containers = Helpers.getObjectMapper().readValue(json, TypeFactory.defaultInstance().constructParametricType(Collection.class, ContainerDTO.class));
            Assume.assumeNotNull(containers);
            for (ContainerDTO container : containers) {
                System.out.println(container);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testContainersWithFields() {
        // this can only be run if you have a fabric running...
        Assume.assumeTrue(Boolean.valueOf(System.getProperty("hasFabric")));

        FabricMBean facade = getFabricMBean();

        String json = facade.containers(Helpers.toList("id"));

        try {
            Collection<ContainerDTO> containers = Helpers.getObjectMapper().readValue(json, TypeFactory.defaultInstance().constructParametricType(Collection.class, ContainerDTO.class));
            Assume.assumeNotNull(containers);
            for (ContainerDTO container : containers) {
                System.out.println(container);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetProfiles() {
        // this can only be run if you have a fabric running...
        Assume.assumeTrue(Boolean.valueOf(System.getProperty("hasFabric")));

        FabricMBean facade = getFabricMBean();

        String json = facade.getProfiles("1.0");

        try {
            Collection<ProfileDTO> profiles = Helpers.getObjectMapper().readValue(json, TypeFactory.defaultInstance().constructParametricType(Collection.class, ProfileDTO.class));
            Assume.assumeNotNull(profiles);
            for (ProfileDTO profile : profiles) {
                System.out.println(profile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetProfilesFields() {
        // this can only be run if you have a fabric running...
        Assume.assumeTrue(Boolean.valueOf(System.getProperty("hasFabric")));

        FabricMBean facade = getFabricMBean();

        String json = facade.getProfiles("1.0", Helpers.toList("id"));

        try {
            Collection<ProfileDTO> profiles = Helpers.getObjectMapper().readValue(json, TypeFactory.defaultInstance().constructParametricType(Collection.class, ProfileDTO.class));
            Assume.assumeNotNull(profiles);
            for (ProfileDTO profile : profiles) {
                System.out.println(profile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetProfileIds() {
        // this can only be run if you have a fabric running...
        Assume.assumeTrue(Boolean.valueOf(System.getProperty("hasFabric")));

        FabricMBean facade = getFabricMBean();

        String json = facade.getProfileIds("1.0");

        try {
            Collection<String> profiles = Helpers.getObjectMapper().readValue(json, TypeFactory.defaultInstance().constructParametricType(Collection.class, String.class));
            Assume.assumeNotNull(profiles);
            for (String profile : profiles) {
                System.out.println("Profile id:" + profile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
