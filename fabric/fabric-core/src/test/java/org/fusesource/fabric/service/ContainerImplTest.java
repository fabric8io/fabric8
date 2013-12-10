/**
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
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

package org.fusesource.fabric.service;

import org.apache.zookeeper.KeeperException;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.DataStore;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.internal.ContainerImpl;
import org.fusesource.fabric.internal.VersionImpl;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ContainerImplTest {

    public static final String CONTAINER_ID = "test";

    static {
        System.setProperty("karaf.data", "target/data");
    }

    FabricService fabricService;
    DataStore dataStore;
    Container container;

    @Before
    public void setUp() {
        fabricService = createMock(FabricService.class);
        dataStore = createMock(DataStore.class);
        expect(fabricService.getDataStore()).andReturn(dataStore).anyTimes();
        container = new ContainerImpl(null, CONTAINER_ID, fabricService);
    }

    @Test
    public void testSetEmptyProfiles() throws Exception {
        String version = "1.0";
        List<String> profiles = Arrays.asList("default");

        expect(dataStore.getContainerVersion(eq(CONTAINER_ID))).andReturn(version).anyTimes();
        expect(dataStore.getContainerProfiles(eq(CONTAINER_ID))).andReturn(profiles).anyTimes();
        dataStore.setContainerProfiles(CONTAINER_ID, profiles);
        expectLastCall().times(2);
        replay(fabricService);
        replay(dataStore);

        container.setProfiles(null);
        container.setProfiles(new Profile[0]);

        verify(fabricService);
        verify(dataStore);
    }

    @Test
    public void testGetWithNoProfile() throws Exception {
        String v = "1.0";
        Version version = new VersionImpl(v, fabricService);
        List<String> profiles = Arrays.asList();

        expect(fabricService.getVersion(eq(v))).andReturn(version).anyTimes();
        expect(dataStore.getContainerVersion(eq(CONTAINER_ID))).andReturn(v).anyTimes();
        expect(dataStore.getContainerProfiles(eq(CONTAINER_ID))).andReturn(profiles).anyTimes();
        expect(dataStore.hasProfile(v, "default")).andReturn(true).anyTimes();
        replay(fabricService);
        replay(dataStore);

        Profile[] p = container.getProfiles();
        assertNotNull(p);
        assertEquals(1, p.length);
        assertEquals(ZkDefs.DEFAULT_PROFILE, p[0].getId());

        verify(fabricService);
        verify(dataStore);

    }

    @Test
    public void testGetSingleProfile() throws Exception {
        String v = "1.0";
        String profileId = "feature-camel";
        Version version = new VersionImpl(v, fabricService);
        List<String> profiles = Arrays.asList(profileId);

        expect(fabricService.getVersion(eq(v))).andReturn(version).anyTimes();
        expect(dataStore.getContainerVersion(eq(CONTAINER_ID))).andReturn(v).anyTimes();
        expect(dataStore.getContainerProfiles(eq(CONTAINER_ID))).andReturn(profiles).anyTimes();
        expect(dataStore.hasProfile(v, profileId)).andReturn(true).anyTimes();
        replay(fabricService);
        replay(dataStore);

        Profile[] p = container.getProfiles();
        assertNotNull(p);
        assertEquals(1, p.length);
        assertEquals(profileId, p[0].getId());

        verify(fabricService);
        verify(dataStore);
    }

    @Test
    public void testGetMultipleProfiles() throws Exception {
        String v = "1.0";
        String profile1Id = "feature-camel";
        String profile2Id = "feature-cxf";
        Version version = new VersionImpl(v, fabricService);
        List<String> profiles = Arrays.asList(profile1Id, profile2Id);

        expect(fabricService.getVersion(eq(v))).andReturn(version).anyTimes();
        expect(dataStore.getContainerVersion(eq(CONTAINER_ID))).andReturn(v).anyTimes();
        expect(dataStore.getContainerProfiles(eq(CONTAINER_ID))).andReturn(profiles).anyTimes();
        expect(dataStore.hasProfile(v, profile1Id)).andReturn(true).anyTimes();
        expect(dataStore.hasProfile(v, profile2Id)).andReturn(true).anyTimes();
        replay(fabricService);
        replay(dataStore);

        Profile[] p = container.getProfiles();
        assertNotNull(p);
        assertEquals(2, p.length);
        assertEquals(profile1Id, p[0].getId());
        assertEquals(profile2Id, p[1].getId());

        verify(fabricService);
        verify(dataStore);
    }

    @Test
    public void testGetContainerProfileOverlay() throws Exception {

        String v = "1.0";
        String defaultProfile = "default";
        String camelProfile = "feature-camel";
        String cxfProfile = "feature-cxf";
        Version version = new VersionImpl(v, fabricService);
        List<String> profiles = Arrays.asList(camelProfile, cxfProfile);

        Map<String, String> defaultAttributes = new HashMap<String, String>();
        Map<String, String> camelAttributes = new HashMap<String, String>();
        Map<String, String> cxfAttributes = new HashMap<String, String>();

        Map<String, byte[]> defaultFiles = new HashMap<String, byte[]>();
        Map<String, byte[]> camelFiles = new HashMap<String, byte[]>();
        Map<String, byte[]> cxfFiles = new HashMap<String, byte[]>();

        Map<String, Map<String, String>> defaultPids = new HashMap<String, Map<String, String>>();
        Map<String, Map<String, String>> camelPids = new HashMap<String, Map<String, String>>();
        Map<String, Map<String, String>> cxfPids = new HashMap<String, Map<String, String>>();

        camelAttributes.put("attribute." + Profile.PARENTS, "default");
        cxfAttributes.put("attribute." + Profile.PARENTS, "feature-camel");
        defaultFiles.put("test1.properties", "key=fromDefault".getBytes());
        camelFiles.put("test1.properties", "key=fromCamel".getBytes());
        cxfFiles.put("test2.properties", "key=fromCxf".getBytes());


        expect(fabricService.getEnvironment()).andReturn("").anyTimes();
        expect(fabricService.getVersion(eq(v))).andReturn(version).anyTimes();

        //Define Attributes
        expect(dataStore.getProfileAttributes(eq(v), eq(defaultProfile))).andReturn(defaultAttributes).anyTimes();
        expect(dataStore.getProfileAttributes(eq(v), eq(camelProfile))).andReturn(camelAttributes).anyTimes();
        expect(dataStore.getProfileAttributes(eq(v), eq(cxfProfile))).andReturn(cxfAttributes).anyTimes();

        //Define Files
        expect(dataStore.getFileConfigurations(eq(v), eq(defaultProfile))).andReturn(defaultFiles).anyTimes();
        expect(dataStore.getFileConfigurations(eq(v), eq(camelProfile))).andReturn(camelFiles).anyTimes();
        expect(dataStore.getFileConfigurations(eq(v), eq(cxfProfile))).andReturn(cxfFiles).anyTimes();

        //Define PIDS
        expect(dataStore.getConfigurations(eq(v), eq(defaultProfile))).andReturn(defaultPids).anyTimes();
        expect(dataStore.getConfigurations(eq(v), eq(camelProfile))).andReturn(camelPids).anyTimes();
        expect(dataStore.getConfigurations(eq(v), eq(cxfProfile))).andReturn(cxfPids).anyTimes();

        dataStore.substituteConfigurations((Map<String, Map<String, String>>) anyObject());
        expectLastCall().anyTimes();
        expect(dataStore.getContainerVersion(eq(CONTAINER_ID))).andReturn(v).anyTimes();
        expect(dataStore.getContainerProfiles(eq(CONTAINER_ID))).andReturn(profiles).anyTimes();
        expect(dataStore.hasProfile(v, camelProfile)).andReturn(true).anyTimes();
        expect(dataStore.hasProfile(v, cxfProfile)).andReturn(true).anyTimes();
        replay(fabricService);
        replay(dataStore);

        Map<String, Map<String, String>> configs = container.getOverlayProfile().getConfigurations();
        assertNotNull(configs);
        assertEquals(2, configs.size());
        assertNotNull(configs.get("test1"));
        assertEquals(1, configs.get("test1").size());
        assertEquals("fromCamel", configs.get("test1").get("key"));
        assertNotNull(configs.get("test2"));
        assertEquals(1, configs.get("test2").size());
        assertEquals("fromCxf", configs.get("test2").get("key"));
        verify(fabricService);
        verify(dataStore);
    }

    @Test(expected = FabricException.class)
    public void testInvalidResolver() throws KeeperException, InterruptedException {
        container.setResolver("invalidreolver");
    }
}
