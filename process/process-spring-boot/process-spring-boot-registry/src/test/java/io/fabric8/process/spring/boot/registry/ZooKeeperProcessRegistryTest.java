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
package io.fabric8.process.spring.boot.registry;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.EnsurePath;
import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static io.fabric8.process.spring.boot.registry.ZooKeeperProcessRegistries.newCurator;
import static org.apache.camel.test.AvailablePortFinder.getNextAvailable;

public class ZooKeeperProcessRegistryTest extends Assert {

    String hosts;

    NIOServerCnxnFactory factory;

    ProcessRegistry registry;

    @Before
    public void before() {
        int zkPort = getNextAvailable(2000);
        hosts = "localhost:" + zkPort;

        factory = ZooKeeperProcessRegistries.zooKeeperServer(zkPort);
        registry = new ZooKeeperProcessRegistry(hosts);
    }

    @After
    public void after() {
        factory.closeAll();
    }

    @Test
    public void shouldReadValue() throws Exception {
        // Given
        String propertyValue = "Hello world!";
        CuratorFramework zk = newCurator(hosts);
        new EnsurePath("/foo/bar/baz").ensure(zk.getZookeeperClient());
        zk.setData().forPath("/foo/bar/baz", propertyValue.getBytes());

        // When
        String readPropertyValue = registry.readProperty("foo.bar.baz");

        // Then
        assertEquals(propertyValue, readPropertyValue);
    }

    @Test
    public void shouldNotReadValue() throws Exception {
        // When
        String readPropertyValue = registry.readProperty("foo.bar.baz");

        // Then
        assertNull(readPropertyValue);
    }

}
