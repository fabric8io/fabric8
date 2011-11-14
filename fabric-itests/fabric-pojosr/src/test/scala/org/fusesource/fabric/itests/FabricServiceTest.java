/*
 * Copyright (C) 2011 FuseSource, Corp. All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the CDDL license
 * a copy of which has been included with this distribution in the license.txt file.
 */
package org.fusesource.fabric.itests;

import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

/**
 *
 */
public class FabricServiceTest extends BaseFabricIntegrationTestSupport {

    protected FabricService service;

    @Before
    public void setFabricService() throws Exception {
        service = getServer().getFabricService();
    }

    @Test
    public void testCreateProfile() throws Exception {

        Profile[] profiles = service.getDefaultVersion().getProfiles();

        Profile test = service.getDefaultVersion().createProfile("testCreateProfile");

        profiles = service.getDefaultVersion().getProfiles();

        assertTrue(Arrays.asList(profiles).contains(test));
    }

}
