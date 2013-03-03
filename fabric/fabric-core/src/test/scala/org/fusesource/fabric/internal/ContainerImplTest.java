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


import org.apache.zookeeper.KeeperException;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.service.FabricServiceImpl;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.junit.Before;
import org.junit.Test;


import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ContainerImplTest {

    FabricServiceImpl fabricService = new FabricServiceImpl();
    Container container = new ContainerImpl(null, "test", fabricService);
    IZKClient izkClient = createMock(IZKClient.class);

    @Before
    public void setUp() {
        fabricService.setZooKeeper(izkClient);
        reset(izkClient);
    }

    @Test
    public void testSetEmptyProfiles() throws KeeperException, InterruptedException {
        String id = "test";
        String version = "1.0";
        expect(izkClient.getStringData(ZkPath.CONFIG_CONTAINER.getPath(id))).andReturn(version).anyTimes();
        expect(izkClient.getStringData(ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, id))).andReturn("default").anyTimes();
        expect(izkClient.setData(ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, id),"default")).andReturn(null).anyTimes();
        replay(izkClient);

        container.setProfiles(null);
        container.setProfiles(new Profile[0]);
        verify(izkClient);
    }

    @Test
    public void testGetWithNoProfile() throws KeeperException, InterruptedException {
        String id = "test";
        String version = "1.0";
        String node = ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, id);
        expect(izkClient.getStringData(ZkPath.CONFIG_CONTAINER.getPath(id))).andReturn(version).anyTimes();
        expect(izkClient.getStringData(node)).andReturn("");
        replay(izkClient);

        Profile[] profiles = container.getProfiles();
        assertNotNull(profiles);
        assertEquals(1, profiles.length);
        assertEquals("default", profiles[0].getId());
        verify(izkClient);
    }

    @Test
    public void testGetSingleProfile() throws KeeperException, InterruptedException {
        String id = "test";
        String version = "1.0";
        String node = ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, id);
        expect(izkClient.getStringData(ZkPath.CONFIG_CONTAINER.getPath(id))).andReturn(version).anyTimes();
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
        String id = "test";
        String version = "1.0";
        String node = ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, id);
        expect(izkClient.getStringData(ZkPath.CONFIG_CONTAINER.getPath(id))).andReturn(version).anyTimes();
        expect(izkClient.getStringData(node)).andReturn("camel esb");
        replay(izkClient);

        Profile[] profiles = container.getProfiles();
        assertNotNull(profiles);
        assertEquals(2, profiles.length);
        assertEquals("camel", profiles[0].getId());
        verify(izkClient);
    }
}
