/*
 * Copyright 2014 original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.etcd.itests;

import io.fabric8.etcd.api.EtcdClient;
import io.fabric8.etcd.api.EtcdException;
import io.fabric8.etcd.api.Response;
import io.fabric8.etcd.core.EtcdClientImpl;
import io.fabric8.etcd.reader.gson.GsonResponseReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

public class SmokeTest {

    private EtcdClient client;

    @Before
    public void setUp() throws URISyntaxException {
        String url = System.getProperty("etcd.url");
        client = new EtcdClientImpl.Builder().baseUri(new URI(url)).responseReader(new GsonResponseReader()).build();
        client.start();
        try {
            client.delete().dir().recursive().forKey("key");
        } catch (EtcdException e) {
            //ignore
        }
    }

    @After
    public void tearDown() {
        try {
            client.delete().forKey("key");
        } catch (EtcdException e) {
            //ignore
        } finally {
            client.stop();
        }
    }

    @Test
    public void testSetGetAndDelete() {
        Response response = client.setData().value("smoke").forKey("key");
        assertNotNull(response);
        assertEquals("smoke", response.getNode().getValue());

        response = client.getData().forKey("key");
        assertValue("smoke", response);

        response = client.delete().forKey("key");
        assertPrevValue("smoke", response);
    }

    @Test(expected = TimeoutException.class)
    public void testSetWatchWithTimeout() throws ExecutionException, InterruptedException, TimeoutException {
        Response response = client.setData().value("smoke").forKey("key");
        assertNotNull(response);
        assertEquals("smoke", response.getNode().getValue());

        response = client.getData().forKey("key");
        assertValue("smoke", response);

        Future<Response> responseFuture = client.getData().watch().forKey("key");
        responseFuture.get(3, TimeUnit.SECONDS);
    }

    @Test
    public void testSetWatchAndDelete() throws ExecutionException, InterruptedException {
        Response response = client.setData().value("smoke").forKey("key");
        assertNotNull(response);
        assertEquals("smoke", response.getNode().getValue());

        response = client.getData().forKey("key");
        assertValue("smoke", response);

        Future<Response> responseFuture = client.getData().watch().forKey("key");

        response = client.setData().value("smoke updated").forKey("key");
        assertValue("smoke updated", response);
        assertPrevValue("smoke", response);

        response = responseFuture.get();
        assertValue("smoke updated", response);
        assertPrevValue("smoke", response);

        response = client.delete().forKey("key");
        assertPrevValue("smoke updated", response);
    }

    @Test
    public void testCreateDirAndChildren() throws URISyntaxException {
        Response response = client.setData().dir().forKey("key");
        assertNotNull(response);

        response = client.setData().value("value1").forKey("key/child1");
        assertNotNull(response);
        assertEquals("value1", response.getNode().getValue());
        response = client.setData().value("value2").forKey("key/clild2");
        assertNotNull(response);
        assertEquals("value2", response.getNode().getValue());

        response = client.getData().recursive().forKey("key/");
        assertNotNull(response);
        assertNotNull(response.getNode().getNodes());
    }

    @Test
    public void testSetGetAndDeleteWithPrevValues() throws URISyntaxException {
        Response response = client.setData().value("smoke").forKey("key");
        assertNotNull(response);
        assertEquals("smoke", response.getNode().getValue());

        response = client.getData().forKey("key");
        assertValue("smoke", response);

        //Set with prev value
        try {
            client.setData().value("smoke updated").prevValue("wrong").forKey("key");
        } catch (EtcdException e) {
            assertEquals(101, e.getErrorCode());
        }

        response = client.setData().value("smoke updated").prevValue("smoke").forKey("key");
        assertPrevValue("smoke", response);

        response = client.delete().forKey("key");
        assertPrevValue("smoke updated", response);
    }

    void assertValue(String expected, Response response) {
        assertNotNull(response);
        assertNotNull(response.getNode());
        assertEquals(expected, response.getNode().getValue());
    }

    void assertPrevValue(String expected, Response response) {
        assertNotNull(response);
        assertNotNull(response.getPrevNode());
        assertEquals(expected, response.getPrevNode().getValue());
    }
}
