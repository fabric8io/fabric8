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
package io.fabric8.etcd.core;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.RecordedRequest;
import io.fabric8.etcd.api.EtcdClient;
import io.fabric8.etcd.api.Keys;
import io.fabric8.etcd.api.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public abstract class AbstractMockWebServerTest {

    private MockWebServer server;
    private EtcdClient client;

    public abstract EtcdClient createClient(MockWebServer server) throws URISyntaxException;

    @Before
    public void setUp() throws URISyntaxException, IOException {
        server = new MockWebServer();
        server.play();
        client = createClient(server);
        client.start();
    }

    @After
    public void tearDown() throws IOException {
        client.stop();
        server.shutdown();
    }

    @Test
    public void testGetRequest() throws Exception {
        server.enqueue(new MockResponse()
                .setBody(Resources.toString(getClass()
                                .getClassLoader()
                                .getResource("get-response-1.json"),
                        Charsets.UTF_8)));

        Response response = client.getData().forKey("message");
        assertNotNull(response);
        assertEquals("get", response.getAction());
        assertEquals("Hello world", response.getNode().getValue());

        RecordedRequest request = server.takeRequest();
        assertEquals("GET",request.getMethod());
        assertTrue(request.getPath().startsWith(Keys.makeKey("message")));
        assertFalse(request.getPath().contains("recursive=true"));
        server.shutdown();
    }

    @Test
    public void testGetAsync() throws Exception {
        server.enqueue(new MockResponse()
                .setBody(Resources.toString(getClass()
                                .getClassLoader()
                                .getResource("get-response-1.json"),
                        Charsets.UTF_8)));

        Future<Response> futureResponse = client.getData().watch().forKey("message");
        Response response = futureResponse.get();
        assertNotNull(response);
        assertEquals("get", response.getAction());
        assertEquals("Hello world", response.getNode().getValue());

        RecordedRequest request = server.takeRequest();
        assertEquals("GET",request.getMethod());
        assertTrue(request.getPath().startsWith(Keys.makeKey("message")));
        assertFalse(request.getPath().contains("recursive=true"));
        server.shutdown();
    }

    @Test
    public void testSetRequest() throws Exception {
        server.enqueue(new MockResponse()
                .setBody(Resources.toString(getClass()
                                .getClassLoader()
                                .getResource("set-response-1.json"),
                        Charsets.UTF_8)));

        Response response = client.setData().value("Hello world").forKey("message");
        assertNotNull(response);
        assertEquals("set", response.getAction());
        assertEquals("Hello world", response.getNode().getValue());

        RecordedRequest request = server.takeRequest();
        assertEquals("PUT",request.getMethod());
        assertTrue(request.getPath().startsWith(Keys.makeKey("message")));
        assertTrue(request.getPath().contains("value=Hello+world"));
        server.shutdown();
    }

    @Test
    public void testCompareAndSwapRequest() throws Exception {
        server.enqueue(new MockResponse()
                .setBody(Resources.toString(getClass()
                                .getClassLoader()
                                .getResource("compare-and-swap-response-2.json"),
                        Charsets.UTF_8)));

        Response response = client.setData().prevValue("Hello world").value("Hello etcd").forKey("message");
        assertNotNull(response);
        assertEquals("compareAndSwap", response.getAction());
        assertEquals("Hello etcd", response.getNode().getValue());

        RecordedRequest request = server.takeRequest();
        assertEquals("PUT",request.getMethod());
        assertTrue(request.getPath().startsWith(Keys.makeKey("message")));
        assertTrue(request.getPath().contains("value=Hello+etcd"));
        assertTrue(request.getPath().contains("prevValue=Hello+world"));
        server.shutdown();
    }


    @Test
    public void testDeleteRequest() throws Exception {
        server.enqueue(new MockResponse()
                .setBody(Resources.toString(getClass()
                                .getClassLoader()
                                .getResource("delete-response-1.json"),
                        Charsets.UTF_8)));

        Response response = client.delete().forKey("message");
        assertNotNull(response);
        assertEquals("delete", response.getAction());
        assertNotNull(response.getNode());
        assertNull(response.getNode().getValue());
        assertNotNull(response.getPrevNode());
        assertEquals("Hello etcd", response.getPrevNode().getValue());

        RecordedRequest request = server.takeRequest();
        assertEquals("DELETE",request.getMethod());
        assertTrue(request.getPath().startsWith(Keys.makeKey("message")));
        server.shutdown();
    }

    @Test
    public void testCompareAndDeleteRequest() throws Exception {
        server.enqueue(new MockResponse()
                .setBody(Resources.toString(getClass()
                                .getClassLoader()
                                .getResource("delete-response-1.json"),
                        Charsets.UTF_8)));

        Response response = client.delete().prevValue("Hello etcd").forKey("message");
        assertNotNull(response);
        assertEquals("delete", response.getAction());
        assertNotNull(response.getNode());
        assertNull(response.getNode().getValue());
        assertNotNull(response.getPrevNode());
        assertEquals("Hello etcd", response.getPrevNode().getValue());

        RecordedRequest request = server.takeRequest();
        assertEquals("DELETE",request.getMethod());
        assertTrue(request.getPath().startsWith(Keys.makeKey("message")));
        assertTrue(request.getPath().contains("prevValue=Hello+etcd"));
        server.shutdown();
    }

}
