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

import java.io.ByteArrayInputStream;
import java.util.*;

import static org.junit.Assert.assertTrue;

/**
 *
 */
public class ProfileTest extends BaseFabricIntegrationTestSupport {

    protected FabricService service;

    @Before
    public void setFabricService() throws Exception {
        service = getServer().getFabricService();
    }

    @Test
    public void testSetAndGetRepositories() throws Exception {
        Profile test = service.getDefaultVersion().createProfile("testGetAndSetRepositories");
        List<String> repos = new ArrayList<String>();
        repos.add("http://foo");
        repos.add("http://bar");
        repos.add("http://cheese");
        test.setRepositories(repos);

        // test getConfigurations
        Map<String, String> rawMapConfig = test.getConfigurations().get("org.fusesource.fabric.agent");
        String keys[] = rawMapConfig.keySet().toArray(new String[0]);
        for (String key : keys) {
            LOG.info(String.format("%s : %s", key, rawMapConfig.get(key)));
            assertTrue(repos.contains(rawMapConfig.remove(key)));
        }
        assertTrue(rawMapConfig.isEmpty());

        // test getFileConfigurations
        byte b [] = test.getFileConfigurations().get("org.fusesource.fabric.agent.properties");
        Properties props = new Properties();
        props.load(new ByteArrayInputStream(b));
        keys = props.keySet().toArray(new String[0]);
        for (String key : keys) {
            LOG.info(String.format("%s : %s", key, props.get(key)));
            assertTrue(repos.contains(props.remove(key).toString()));
        }
        assertTrue(props.isEmpty());

    }
}
