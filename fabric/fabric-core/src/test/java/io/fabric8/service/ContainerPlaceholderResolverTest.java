/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
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
package io.fabric8.service;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import io.fabric8.api.Container;
import io.fabric8.api.DataStore;
import io.fabric8.api.FabricService;
import io.fabric8.api.PlaceholderResolver;
import io.fabric8.internal.ContainerImpl;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;

public class ContainerPlaceholderResolverTest {

    private FabricService fabricService;
    private DataStore dataStore;

    String ip = "10.0.0.0";
    String localhostname = "localhost";
    String bindaddress = "0.0.0.0";
    String containerResolver = "localhostname";

    @Before
    public void setUp() {
        fabricService = createMock(FabricService.class);
        dataStore = createMock(DataStore.class);
        expect(fabricService.getCurrentContainerName()).andReturn("root").anyTimes();
        expect(fabricService.getDataStore()).andReturn(dataStore).anyTimes();

        expect(fabricService.getContainer(EasyMock.<String>anyObject())).andStubAnswer(new IAnswer<Container>() {
            @Override
            public Container answer() throws Throwable {
                return new ContainerImpl(null, (String) EasyMock.getCurrentArguments()[0], fabricService);
            }
        });

        expect(dataStore.getContainerAttribute(eq("root"), eq(DataStore.ContainerAttribute.Ip), eq(""), eq(false), eq(true))).andReturn(ip).anyTimes();
        expect(dataStore.getContainerAttribute(eq("root"), eq(DataStore.ContainerAttribute.LocalHostName), eq(""), eq(false), eq(true))).andReturn(localhostname).anyTimes();
        expect(dataStore.getContainerAttribute(eq("root"), eq(DataStore.ContainerAttribute.BindAddress), eq(""), eq(false), eq(true))).andReturn(bindaddress).anyTimes();
        expect(dataStore.getContainerAttribute(eq("root"), eq(DataStore.ContainerAttribute.Resolver), eq(""), eq(false), eq(true))).andReturn(containerResolver).anyTimes();

        replay(fabricService);
        replay(dataStore);
    }

    @Test
    public void testResolveCurrentName() throws Exception {
        final FabricService fabricService = createMock(FabricService.class);
        final DataStore dataStore = createMock(DataStore.class);
        expect(fabricService.getCurrentContainerName()).andReturn("root").anyTimes();
        expect(fabricService.getContainer(EasyMock.<String > anyObject())).andReturn(new ContainerImpl(null, "root", fabricService));

        replay(fabricService);
        replay(dataStore);

        PlaceholderResolver resolver = getContainerPlaceholderResolver();
        assertEquals("root",resolver.resolve(fabricService, null, null, null, "container:name"));
        verify(fabricService);
        verify(dataStore);
    }

    @Test
    public void testResolveCurrentAttributes() throws Exception {
        PlaceholderResolver resolver = getContainerPlaceholderResolver();
        assertEquals(ip,resolver.resolve(fabricService, null, null, null, "container:ip"));
        assertEquals(localhostname,resolver.resolve(fabricService, null, null, null, "container:localhostname"));
        assertEquals(bindaddress,resolver.resolve(fabricService, null, null, null, "container:bindaddress"));
        assertEquals(containerResolver,resolver.resolve(fabricService, null, null, null, "container:resolver"));

        verify(fabricService);
        verify(dataStore);
    }

    @Test
    public void testResolveNameContainerAttributes() throws Exception {
        PlaceholderResolver resolver = getContainerPlaceholderResolver();
        assertEquals(ip,resolver.resolve(fabricService, null, null, null, "container:root/ip"));
        assertEquals(localhostname,resolver.resolve(fabricService, null, null, null, "container:root/localhostname"));
        assertEquals(bindaddress,resolver.resolve(fabricService, null, null, null, "container:root/bindaddress"));
        assertEquals(containerResolver,resolver.resolve(fabricService, null, null, null, "container:root/resolver"));

        verify(fabricService);
        verify(dataStore);
    }

    @Test
    public void testResolveAttributeCase() throws Exception {
        PlaceholderResolver resolver = getContainerPlaceholderResolver();
        assertEquals(ip,resolver.resolve(fabricService, null, null, null, "container:root/IP"));
        assertEquals(localhostname,resolver.resolve(fabricService, null, null, null, "container:root/LocalHostName"));
        assertEquals(bindaddress,resolver.resolve(fabricService, null, null, null, "container:root/Bindaddress"));
        assertEquals(containerResolver,resolver.resolve(fabricService, null, null, null, "container:root/Resolver"));

        verify(fabricService);
        verify(dataStore);
    }

    private PlaceholderResolver getContainerPlaceholderResolver() {
        ContainerPlaceholderResolver resolver = new ContainerPlaceholderResolver();
        resolver.activate();
        return resolver;
    }
}
