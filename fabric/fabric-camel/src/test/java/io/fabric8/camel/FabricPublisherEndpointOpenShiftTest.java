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
package io.fabric8.camel;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import io.fabric8.groups.Group;
import io.fabric8.groups.GroupListener;
import org.apache.curator.framework.CuratorFramework;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class FabricPublisherEndpointOpenShiftTest {

    private FabricPublisherEndpoint endpoint;
    private String karafName;

    @Before
    public void init() throws Exception {
        karafName = System.setProperty("karaf.name", "root");
        FabricComponent component = new FabricComponent() {
            @Override
            public CuratorFramework getCurator() {
                return null;
            }

            @Override
            public Group<CamelNodeState> createGroup(String path) {
                return new Group<CamelNodeState>() {
                    @Override
                    public boolean isConnected() {
                        return false;
                    }

                    @Override
                    public void start() {
                    }

                    @Override
                    public void close() throws IOException {
                    }

                    @Override
                    public void add(GroupListener<CamelNodeState> listener) {
                    }

                    @Override
                    public void remove(GroupListener<CamelNodeState> listener) {
                    }

                    @Override
                    public void update(CamelNodeState state) {
                    }

                    @Override
                    public Map<String, CamelNodeState> members() {
                        return null;
                    }

                    @Override
                    public boolean isMaster() {
                        return false;
                    }

                    @Override
                    public CamelNodeState master() {
                        return null;
                    }

                    @Override
                    public List<CamelNodeState> slaves() {
                        return null;
                    }

                    @Override
                    public CamelNodeState getLastState() {
                        return null;
                    }
                };
            }
        };
        endpoint = new FabricPublisherEndpoint("http://localhost", component, null, "child") {
            @Override
            protected int publicPort(URI uri) {
                return uri.getPort() + 42;
            }
        };
    }

    @After
    public void cleanup() {
        if (karafName != null) {
            System.setProperty("karaf.name", karafName);
        }
    }

    @Test
    public void testPortMapping() throws Exception {
        assertThat(endpoint.toPublicAddress("http:jetty://localhost:1/x/y?a=b&c=d"), equalTo("http:jetty://localhost:43/x/y?a=b&c=d"));
        assertThat(endpoint.toPublicAddress("http:jetty:a:b://localhost:1/x/y?a=b"), equalTo("http:jetty:a:b://localhost:43/x/y?a=b"));
        assertThat(endpoint.toPublicAddress("http://localhost:1"), equalTo("http://localhost:43"));
    }

}
