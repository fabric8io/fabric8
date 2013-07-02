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

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.ExistsBuilder;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.curator.framework.api.SetDataBuilder;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.easymock.EasyMock;
import org.easymock.classextension.ConstructorArgs;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.service.FabricServiceImpl;
import org.fusesource.fabric.service.ZooKeeperDataStore;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import scala.actors.threadpool.Arrays;

import java.util.Collections;
import java.util.Map;

import static org.easymock.classextension.EasyMock.*;
import static org.fusesource.fabric.zookeeper.ZkDefs.DEFAULT_PROFILE;
import static org.fusesource.fabric.zookeeper.ZkPath.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

// TODO - see what the deal with this test is...
@Ignore
public class ContainerImplTest {

    public static final String CONTAINER_ID = "test";

    static {
        System.setProperty("karaf.data", "target/data");
    }

    FabricServiceImpl fabricService = new FabricServiceImpl();
    //Container container = new ContainerImpl(null, CONTAINER_ID, fabricService);
    Container container = createMock(ContainerImpl.class, new ConstructorArgs(ContainerImpl.class.getDeclaredConstructors()[0], null, CONTAINER_ID, fabricService));
    CuratorFramework curator = createMock(CuratorFramework.class);

    @Before
    public void setUp() {
        ZooKeeperDataStore zooKeeperDataStore = new ZooKeeperDataStore();
        zooKeeperDataStore.setCurator(curator);
        fabricService.setDataStore(zooKeeperDataStore);
        fabricService.setCurator(curator);

        // how did this ever work?
        reset(container);
    }

    @Test
    public void testSetEmptyProfiles() throws Exception {
        String id = CONTAINER_ID;
        String version = "1.0";
        GetDataBuilder getBuilder = createMock(GetDataBuilder.class);
        SetDataBuilder setBuilder = createMock(SetDataBuilder.class);

        expect(getBuilder.forPath(CONFIG_CONTAINER.getPath(id))).andReturn(version.getBytes()).anyTimes();
        expect(getBuilder.forPath(CONFIG_VERSIONS_CONTAINER.getPath(version, id))).andReturn(DEFAULT_PROFILE.getBytes()).anyTimes();
        expect(setBuilder.forPath(eq(CONFIG_VERSIONS_CONTAINER.getPath(version, id)), (byte[]) anyObject())).andReturn(null).anyTimes();
        expect(curator.getData()).andReturn(getBuilder).anyTimes();
        expect(curator.setData()).andReturn(setBuilder).anyTimes();

        replay(getBuilder);
        replay(setBuilder);
        replay(curator);

        container.setProfiles(null);
        container.setProfiles(new Profile[0]);
        verify(curator);
        verify(getBuilder);
        verify(setBuilder);
    }

    @Test
    public void testGetWithNoProfile() throws Exception {
        String id = CONTAINER_ID;
        String version = "1.0";

        String node = ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, id);
        GetDataBuilder getBuilder = createMock(GetDataBuilder.class);
        SetDataBuilder setBuilder = createMock(SetDataBuilder.class);
        ExistsBuilder existsBuilder = createMock(ExistsBuilder.class);

        expect(getBuilder.forPath(CONFIG_CONTAINER.getPath(id))).andReturn(version.getBytes()).anyTimes();
        expect(getBuilder.forPath(node)).andReturn("".getBytes()).anyTimes();
        expect(setBuilder.forPath(eq(CONFIG_VERSIONS_CONTAINER.getPath(version, id)), (byte[]) anyObject())).andReturn(null).anyTimes();
        expect(existsBuilder.forPath(EasyMock.<String>anyObject())).andReturn(new Stat()).anyTimes();
        expect(curator.getData()).andReturn(getBuilder).anyTimes();
        expect(curator.setData()).andReturn(setBuilder).anyTimes();
        expect(curator.checkExists()).andReturn(existsBuilder).anyTimes();

        replay(getBuilder);
        replay(setBuilder);
        replay(existsBuilder);
        replay(curator);

        Profile[] profiles = container.getProfiles();
        assertNotNull(profiles);
        assertEquals(1, profiles.length);
        assertEquals(ZkDefs.DEFAULT_PROFILE, profiles[0].getId());
        verify(curator);
        verify(getBuilder);
        verify(setBuilder);
        verify(existsBuilder);
    }

    @Test
    public void testGetSingleProfile() throws Exception {
        String id = CONTAINER_ID;
        String version = "1.0";
        String node = ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, id);

        GetDataBuilder getBuilder = createMock(GetDataBuilder.class);
        SetDataBuilder setBuilder = createMock(SetDataBuilder.class);
        ExistsBuilder existsBuilder = createMock(ExistsBuilder.class);

        expect(getBuilder.forPath(CONFIG_CONTAINER.getPath(id))).andReturn(version.getBytes()).anyTimes();
        expect(getBuilder.forPath(node)).andReturn("camel".getBytes()).anyTimes();
        expect(setBuilder.forPath(eq(CONFIG_VERSIONS_CONTAINER.getPath(version, id)), (byte[]) anyObject())).andReturn(null).anyTimes();
        expect(existsBuilder.forPath(EasyMock.<String>anyObject())).andReturn(new Stat()).anyTimes();
        expect(curator.getData()).andReturn(getBuilder).anyTimes();
        expect(curator.setData()).andReturn(setBuilder).anyTimes();
        expect(curator.checkExists()).andReturn(existsBuilder).anyTimes();

        replay(getBuilder);
        replay(setBuilder);
        replay(existsBuilder);
        replay(curator);

        Profile[] profiles = container.getProfiles();
        assertNotNull(profiles);
        assertEquals(1, profiles.length);
        assertEquals("camel", profiles[0].getId());
        verify(curator);
        verify(getBuilder);
        verify(setBuilder);
        verify(existsBuilder);
    }

    @Test
    public void testGetMultipleProfiles() throws Exception {
        String id = CONTAINER_ID;
        String version = "1.0";
        String node = ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, id);

        GetDataBuilder getBuilder = createMock(GetDataBuilder.class);
        SetDataBuilder setBuilder = createMock(SetDataBuilder.class);
        ExistsBuilder existsBuilder = createMock(ExistsBuilder.class);

        expect(getBuilder.forPath(CONFIG_CONTAINER.getPath(id))).andReturn(version.getBytes()).anyTimes();
        expect(getBuilder.forPath(node)).andReturn("camel esb".getBytes()).anyTimes();
        expect(setBuilder.forPath(eq(CONFIG_VERSIONS_CONTAINER.getPath(version, id)), (byte[]) anyObject())).andReturn(null).anyTimes();
        expect(existsBuilder.forPath(EasyMock.<String>anyObject())).andReturn(new Stat()).anyTimes();
        expect(curator.getData()).andReturn(getBuilder).anyTimes();
        expect(curator.setData()).andReturn(setBuilder).anyTimes();
        expect(curator.checkExists()).andReturn(existsBuilder).anyTimes();

        replay(getBuilder);
        replay(setBuilder);
        replay(existsBuilder);
        replay(curator);

        Profile[] profiles = container.getProfiles();
        assertNotNull(profiles);
        assertEquals(2, profiles.length);
        assertEquals("camel", profiles[0].getId());
        assertEquals("esb", profiles[1].getId());
        verify(curator);
        verify(getBuilder);
        verify(setBuilder);
        verify(existsBuilder);
    }

    @Test
    public void testGetContainerProfileOverlay() throws Exception {
        String id = CONTAINER_ID;
        String version = "1.0";


        GetDataBuilder getBuilder = createMock(GetDataBuilder.class);
        SetDataBuilder setBuilder = createMock(SetDataBuilder.class);
        ExistsBuilder existsBuilder = createMock(ExistsBuilder.class);
        GetChildrenBuilder getChildrenBuilder = createMock(GetChildrenBuilder.class);

        expect(getBuilder.forPath(CONFIG_CONTAINER.getPath(id))).andReturn(version.getBytes()).anyTimes();
        expect(getBuilder.forPath(ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, id))).andReturn("esb".getBytes()).anyTimes();
        expect(getBuilder.forPath(eq(CONFIG_VERSIONS_PROFILE.getPath(version, "esb")))).andReturn("parents=cxf camel\n".getBytes()).anyTimes();
        expect(getBuilder.forPath(eq(CONFIG_VERSIONS_PROFILE.getPath(version, "cxf")))).andReturn(null).anyTimes();
        expect(getBuilder.forPath(eq(CONFIG_VERSIONS_PROFILE.getPath(version, "camel")))).andReturn(null).anyTimes();

        expect(getChildrenBuilder.forPath(eq(CONFIG_VERSIONS_PROFILE.getPath(version, "esb")))).andReturn(Collections.<String>emptyList()).anyTimes();
        expect(getChildrenBuilder.forPath(eq(CONFIG_VERSIONS_PROFILE.getPath(version, "cxf")))).andReturn(Arrays.asList(new String[] { "pid1.properties", "pid2.properties" })).anyTimes();
        expect(getChildrenBuilder.forPath(eq(CONFIG_VERSIONS_PROFILE.getPath(version, "camel")))).andReturn(Arrays.asList(new String[]{"pid1.properties"})).anyTimes();

        expect(getBuilder.forPath(eq(CONFIG_VERSIONS_PROFILE.getPath(version, "cxf")+ "/pid1.properties" ))).andReturn("k1=v1\nk2=v2".getBytes()).anyTimes();
        expect(getBuilder.forPath(eq(CONFIG_VERSIONS_PROFILE.getPath(version, "cxf")+ "/pid2.properties"))).andReturn("k3=v3".getBytes()).anyTimes();
        expect(getBuilder.forPath(eq(CONFIG_VERSIONS_PROFILE.getPath(version, "camel") + "/pid1.properties"))).andReturn("k1=v4".getBytes()).anyTimes();

        expect(existsBuilder.forPath(EasyMock.<String>anyObject())).andReturn(new Stat()).anyTimes();
        expect(curator.getData()).andReturn(getBuilder).anyTimes();
        expect(curator.setData()).andReturn(setBuilder).anyTimes();
        expect(curator.getChildren()).andReturn(getChildrenBuilder).anyTimes();
        expect(curator.checkExists()).andReturn(existsBuilder).anyTimes();

        replay(getBuilder);
        replay(setBuilder);
        replay(existsBuilder);
        replay(getChildrenBuilder);
        replay(curator);

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
        verify(curator);
        verify(getBuilder);
        verify(setBuilder);
        verify(getChildrenBuilder);
        verify(existsBuilder);
    }

    @Test(expected = FabricException.class)
    public void testInvalidResolver() throws KeeperException, InterruptedException {
        container.setResolver("invalidreolver");
    }
}
