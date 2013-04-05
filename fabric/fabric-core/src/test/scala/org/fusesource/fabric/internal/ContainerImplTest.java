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

package org.fusesource.fabric.internal;


import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.easymock.EasyMock;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.service.FabricServiceImpl;
import org.fusesource.fabric.service.ZooKeeperDataStore;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.junit.Before;
import org.junit.Test;
import scala.actors.threadpool.Arrays;


import java.util.Collections;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ContainerImplTest {

    public static final String CONTAINER_ID = "test";

    FabricServiceImpl fabricService = new FabricServiceImpl();
    Container container = new ContainerImpl(null, CONTAINER_ID, fabricService);
    IZKClient izkClient = createMock(IZKClient.class);

    @Before
    public void setUp() {
        ZooKeeperDataStore zooKeeperDataStore = new ZooKeeperDataStore();
        zooKeeperDataStore.setZk(izkClient);
        fabricService.setDataStore(zooKeeperDataStore);
        fabricService.setZooKeeper(izkClient);
        reset(izkClient);
    }

    @Test
    public void testSetEmptyProfiles() throws KeeperException, InterruptedException {
        String id = CONTAINER_ID;
        String version = "1.0";
        expect(izkClient.getStringData(ZkPath.CONFIG_CONTAINER.getPath(id))).andReturn(version).anyTimes();
        expect(izkClient.getStringData(ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, id))).andReturn(ZkDefs.DEFAULT_PROFILE).anyTimes();
        expect(izkClient.createOrSetWithParents(ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, id), ZkDefs.DEFAULT_PROFILE, CreateMode.PERSISTENT)).andReturn(null).anyTimes();
        replay(izkClient);

        container.setProfiles(null);
        container.setProfiles(new Profile[0]);
        verify(izkClient);
    }

    @Test
    public void testGetWithNoProfile() throws KeeperException, InterruptedException {
        String id = CONTAINER_ID;
        String version = "1.0";
        String node = ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, id);
        expect(izkClient.getStringData(ZkPath.CONFIG_CONTAINER.getPath(id))).andReturn(version).anyTimes();
        expect(izkClient.exists(ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, ZkDefs.DEFAULT_PROFILE))).andReturn(new Stat()).anyTimes();
        expect(izkClient.getStringData(node)).andReturn("");
        replay(izkClient);

        Profile[] profiles = container.getProfiles();
        assertNotNull(profiles);
        assertEquals(1, profiles.length);
        assertEquals(ZkDefs.DEFAULT_PROFILE, profiles[0].getId());
        verify(izkClient);
    }

    @Test
    public void testGetSingleProfile() throws KeeperException, InterruptedException {
        String id = CONTAINER_ID;
        String version = "1.0";
        String node = ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, id);
        expect(izkClient.getStringData(ZkPath.CONFIG_CONTAINER.getPath(id))).andReturn(version).anyTimes();
        expect(izkClient.exists(ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "camel"))).andReturn(new Stat()).anyTimes();
        expect(izkClient.getStringData(node)).andReturn("camel");
        replay(izkClient);

        Profile[] profiles = container.getProfiles();
        assertNotNull(profiles);
        assertEquals(1, profiles.length);
        assertEquals("camel", profiles[0].getId());
        verify(izkClient);
    }

    @Test
    public void testGetMultipleProfiles() throws KeeperException, InterruptedException {
        String id = CONTAINER_ID;
        String version = "1.0";
        String node = ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, id);
        expect(izkClient.getStringData(ZkPath.CONFIG_CONTAINER.getPath(id))).andReturn(version).anyTimes();
        expect(izkClient.exists(ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "camel"))).andReturn(new Stat()).anyTimes();
        expect(izkClient.exists(ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "esb"))).andReturn(new Stat()).anyTimes();
        expect(izkClient.getStringData(node)).andReturn("camel esb");
        replay(izkClient);

        Profile[] profiles = container.getProfiles();
        assertNotNull(profiles);
        assertEquals(2, profiles.length);
        assertEquals("camel", profiles[0].getId());
        assertEquals("esb", profiles[1].getId());
        verify(izkClient);
    }

    @Test
    public void testGetContainerProfileOverlay() throws KeeperException, InterruptedException {
        String id = CONTAINER_ID;
        String version = "1.0";
        expect(izkClient.getStringData(ZkPath.CONFIG_CONTAINER.getPath(id))).andReturn(version).anyTimes();
        expect(izkClient.exists(ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "camel"))).andReturn(new Stat()).anyTimes();
        expect(izkClient.exists(ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "cxf"))).andReturn(new Stat()).anyTimes();
        expect(izkClient.exists(ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "esb"))).andReturn(new Stat()).anyTimes();
        expect(izkClient.getStringData(ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, id))).andReturn("esb").anyTimes();
        expect(izkClient.getStringData(ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "esb"))).andReturn("parents=cxf camel\n").anyTimes();
        expect(izkClient.getStringData(ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "cxf"))).andReturn(null).anyTimes();
        expect(izkClient.getStringData(ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "camel"))).andReturn(null).anyTimes();

        expect(izkClient.getChildren(ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "esb"))).andReturn(Collections.<String>emptyList()).anyTimes();
        expect(izkClient.getChildren(ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "cxf"))).andReturn(Arrays.asList(new String[] { "pid1.properties", "pid2.properties" })).anyTimes();
        expect(izkClient.getChildren(ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "camel"))).andReturn(Arrays.asList(new String[]{"pid1.properties"})).anyTimes();

        expect(izkClient.exists(ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "cxf") + "/pid1.properties")).andReturn(new Stat()).anyTimes();
        expect(izkClient.exists(ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "cxf") + "/pid2.properties")).andReturn(new Stat()).anyTimes();
        expect(izkClient.exists(ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "camel") + "/pid1.properties")).andReturn(new Stat()).anyTimes();

        expect(izkClient.getData(ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "cxf") + "/pid1.properties")).andReturn("k1=v1\nk2=v2".getBytes()).anyTimes();
        expect(izkClient.getData(ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "cxf") + "/pid2.properties")).andReturn("k3=v3".getBytes()).anyTimes();
        expect(izkClient.getData(ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "camel") + "/pid1.properties")).andReturn("k1=v4".getBytes()).anyTimes();

        replay(izkClient);

        Map<String, Map<String, String>> configs = container.getOverlayProfile().getConfigurations();
        assertNotNull(configs);
        assertEquals(2, configs.size());
        assertNotNull(configs.get("pid1"));
        assertEquals(2, configs.get("pid1").size());
        assertEquals("v4", configs.get("pid1").get("k1"));
        assertEquals("v2", configs.get("pid1").get("k2"));
        assertNotNull(configs.get("pid2"));
        assertEquals(1, configs.get("pid2").size());
        assertEquals("v3", configs.get("pid2").get("k3"));
        verify(izkClient);
    }

    @Test(expected = FabricException.class)
    public void testInvalidResolver() throws KeeperException, InterruptedException {
        container.setResolver("invalidreolver");
    }
}
