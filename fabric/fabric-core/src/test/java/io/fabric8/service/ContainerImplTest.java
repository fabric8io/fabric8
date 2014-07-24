/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.service;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import io.fabric8.api.Container;
import io.fabric8.api.DataStore;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileRegistry;
import io.fabric8.api.ProfileService;
import io.fabric8.api.Version;
import io.fabric8.api.ZkDefs;
import io.fabric8.internal.ContainerImpl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.zookeeper.KeeperException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ContainerImplTest {

    public static final String CONTAINER_ID = "test";

    static {
        System.setProperty("karaf.data", "target/data");
    }

    FabricService fabricService;
    ProfileService profileService;
    ProfileRegistry profileRegistry;
    DataStore dataStore;
    Container container;

    @Before
    public void setUp() {
        fabricService = createMock(FabricService.class);
        dataStore = createMock(DataStore.class);
        expect(fabricService.adapt(DataStore.class)).andReturn(dataStore).anyTimes();
        container = new ContainerImpl(null, CONTAINER_ID, fabricService);
    }

    @Test
    @Ignore("[FABRIC-1110] Mocked test makes invalid assumption on the implementation")
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
    @Ignore("[FABRIC-1110] Mocked test makes invalid assumption on the implementation")
    public void testGetWithNoProfile() throws Exception {
        String v = "1.0";
        Version version = null; //new VersionImpl(v, fabricService);
        List<String> profiles = Arrays.asList();

        expect(profileService.getRequiredVersion(eq(v))).andReturn(version).anyTimes();
        expect(dataStore.getContainerVersion(eq(CONTAINER_ID))).andReturn(v).anyTimes();
        expect(dataStore.getContainerProfiles(eq(CONTAINER_ID))).andReturn(profiles).anyTimes();
        expect(profileRegistry.hasProfile(v, "default")).andReturn(true).anyTimes();
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
    @Ignore("[FABRIC-1110] Mocked test makes invalid assumption on the implementation")
    public void testGetSingleProfile() throws Exception {
        String v = "1.0";
        String profileId = "feature-camel";
        Version version = null; //new VersionImpl(v, fabricService);
        List<String> profiles = Arrays.asList(profileId);

        expect(profileService.getRequiredVersion(eq(v))).andReturn(version).anyTimes();
        expect(dataStore.getContainerVersion(eq(CONTAINER_ID))).andReturn(v).anyTimes();
        expect(dataStore.getContainerProfiles(eq(CONTAINER_ID))).andReturn(profiles).anyTimes();
        expect(profileRegistry.hasProfile(v, profileId)).andReturn(true).anyTimes();
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
    @Ignore("[FABRIC-1110] Mocked test makes invalid assumption on the implementation")
    public void testGetMultipleProfiles() throws Exception {
        String v = "1.0";
        String profile1Id = "feature-camel";
        String profile2Id = "feature-cxf";
        Version version = null; //new VersionImpl(v, fabricService);
        List<String> profiles = Arrays.asList(profile1Id, profile2Id);

        expect(profileService.getRequiredVersion(eq(v))).andReturn(version).anyTimes();
        expect(dataStore.getContainerVersion(eq(CONTAINER_ID))).andReturn(v).anyTimes();
        expect(dataStore.getContainerProfiles(eq(CONTAINER_ID))).andReturn(profiles).anyTimes();
        expect(profileRegistry.hasProfile(v, profile1Id)).andReturn(true).anyTimes();
        expect(profileRegistry.hasProfile(v, profile2Id)).andReturn(true).anyTimes();
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


    //We should be able to remove a profile that doesn't exist from a container.
    //A missing profile may be added to a container during startup (not possible to validate) or after an upgrade / rollback operation.
    @Test
    @Ignore("[FABRIC-1110] Mocked test makes invalid assumption on the implementation")
    public void testRemoveMissingProfile() throws Exception {
        String v = "1.0";
        String profile1Id = "feature-camel";
        String profile2Id = "feature-cxf";
        String missing = "missing";
        Version version = null; //new VersionImpl(v, fabricService);
        List<String> profiles = Arrays.asList(profile1Id, profile2Id, missing);
        List<String> profilesToSet = Arrays.asList(profile1Id, profile2Id);

        expect(profileService.getRequiredVersion(eq(v))).andReturn(version).anyTimes();
        expect(dataStore.getContainerVersion(eq(CONTAINER_ID))).andReturn(v).anyTimes();
        expect(dataStore.getContainerProfiles(eq(CONTAINER_ID))).andReturn(profiles).anyTimes();
        expect(profileRegistry.hasProfile(v, profile1Id)).andReturn(true).anyTimes();
        expect(profileRegistry.hasProfile(v, profile2Id)).andReturn(true).anyTimes();
        expect(profileRegistry.hasProfile(v, missing)).andReturn(false).anyTimes();
        //expect(profileRegistry.getProfileAttributes(eq(v), EasyMock.<String>anyObject())).andReturn(Maps.<String, String>newHashMap()).anyTimes();
        dataStore.setContainerProfiles(eq(CONTAINER_ID), eq(profilesToSet));
        expectLastCall().once();
        replay(fabricService);
        replay(dataStore);

        container.removeProfiles(missing);

        verify(fabricService);
        verify(dataStore);
    }

    @Test
    @Ignore("[FABRIC-1110] Mocked test makes invalid assumption on the implementation")
    public void testContainerProfileWithMissingParents() throws Exception {
        String v = "1.0";
        String profile1Id = "feature-camel";
        String profile2Id = "feature-cxf";
        String missing = "missing";
        Version version = null; //new VersionImpl(v, fabricService);
        List<String> profiles = Arrays.asList(profile1Id, profile2Id, missing);

        expect(profileService.getRequiredVersion(eq(v))).andReturn(version).anyTimes();
        expect(fabricService.getEnvironment()).andReturn("").anyTimes();
        expect(dataStore.getContainerVersion(eq(CONTAINER_ID))).andReturn(v).anyTimes();
        expect(dataStore.getContainerProfiles(eq(CONTAINER_ID))).andReturn(profiles).anyTimes();
        expect(profileRegistry.hasProfile(v, profile1Id)).andReturn(true).anyTimes();
        expect(profileRegistry.hasProfile(v, profile2Id)).andReturn(true).anyTimes();
        expect(profileRegistry.hasProfile(v, missing)).andReturn(false).anyTimes();
        //expect(profileRegistry.getProfileAttributes(eq(v), EasyMock.<String>anyObject())).andReturn(Maps.<String, String>newHashMap()).anyTimes();
        replay(fabricService);
        replay(dataStore);

        Profile overlay = container.getOverlayProfile();
        List<Profile> parents = overlay.getParents();
        assertEquals(2, parents.size());

        verify(fabricService);
        verify(dataStore);
    }

    @Test
    @Ignore("[FABRIC-1110] Mocked test makes invalid assumption on the implementation")
    public void testGetContainerProfileOverlay() throws Exception {

        String v = "1.0";
        String defaultProfile = "default";
        String camelProfile = "feature-camel";
        String cxfProfile = "feature-cxf";
        Version version = null; //new VersionImpl(v, fabricService);
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
        expect(profileService.getRequiredVersion(eq(v))).andReturn(version).anyTimes();

        //Define Attributes
        //expect(profileRegistry.getProfileAttributes(eq(v), eq(defaultProfile))).andReturn(defaultAttributes).anyTimes();
        //expect(profileRegistry.getProfileAttributes(eq(v), eq(camelProfile))).andReturn(camelAttributes).anyTimes();
        //expect(profileRegistry.getProfileAttributes(eq(v), eq(cxfProfile))).andReturn(cxfAttributes).anyTimes();

        //Define Files
        expect(profileRegistry.getFileConfigurations(eq(v), eq(defaultProfile))).andReturn(defaultFiles).anyTimes();
        expect(profileRegistry.getFileConfigurations(eq(v), eq(camelProfile))).andReturn(camelFiles).anyTimes();
        expect(profileRegistry.getFileConfigurations(eq(v), eq(cxfProfile))).andReturn(cxfFiles).anyTimes();

        //Define PIDS
        expect(profileRegistry.getConfigurations(eq(v), eq(defaultProfile))).andReturn(defaultPids).anyTimes();
        expect(profileRegistry.getConfigurations(eq(v), eq(camelProfile))).andReturn(camelPids).anyTimes();
        expect(profileRegistry.getConfigurations(eq(v), eq(cxfProfile))).andReturn(cxfPids).anyTimes();

        fabricService.substituteConfigurations((Map<String, Map<String, String>>) anyObject());
        expectLastCall().anyTimes();
        expect(dataStore.getContainerVersion(eq(CONTAINER_ID))).andReturn(v).anyTimes();
        expect(dataStore.getContainerProfiles(eq(CONTAINER_ID))).andReturn(profiles).anyTimes();
        expect(profileRegistry.hasProfile(v, camelProfile)).andReturn(true).anyTimes();
        expect(profileRegistry.hasProfile(v, cxfProfile)).andReturn(true).anyTimes();
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
