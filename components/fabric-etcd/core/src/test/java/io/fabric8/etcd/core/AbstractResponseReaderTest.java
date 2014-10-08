/*
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.etcd.core;

import io.fabric8.etcd.api.Response;
import io.fabric8.etcd.api.ResponseReader;
import org.junit.Test;

import static org.junit.Assert.*;

public abstract class AbstractResponseReaderTest {

    public abstract ResponseReader getResponseReader();

    @Test
    public void testReadSetResponse() throws Exception {
        Response response = getResponseReader().read(getClass().getClassLoader().getResourceAsStream("set-response-1.json"));
        assertNotNull(response);
        assertEquals("set", response.getAction());
        assertNotNull(response.getNode());
        assertEquals("/message", response.getNode().getKey());
        assertEquals("Hello world", response.getNode().getValue());
    }

    @Test
    public void testReadSetUpdateResponse() throws Exception {
        Response response = getResponseReader().read(getClass().getClassLoader().getResourceAsStream("compare-and-swap-response-2.json"));
        assertNotNull(response);
        assertEquals("compareAndSwap", response.getAction());
        assertNotNull(response.getNode());
        assertEquals("/message", response.getNode().getKey());
        assertEquals("Hello etcd", response.getNode().getValue());
        assertNotNull(response.getPrevNode());
        assertEquals("/message", response.getPrevNode().getKey());
        assertEquals("Hello world", response.getPrevNode().getValue());
    }

    @Test
    public void testReadGetResponse() throws Exception {
        Response response = getResponseReader().read(getClass().getClassLoader().getResourceAsStream("get-response-1.json"));
        assertNotNull(response);
        assertEquals("get", response.getAction());
        assertNotNull(response.getNode());
        assertEquals("/message", response.getNode().getKey());
        assertEquals("Hello world", response.getNode().getValue());
    }

    @Test
    public void testReadDeleteResponse() throws Exception {
        Response response = getResponseReader().read(getClass().getClassLoader().getResourceAsStream("delete-response-1.json"));
        assertNotNull(response);
        assertEquals("delete", response.getAction());
        assertEquals("/message", response.getNode().getKey());
        assertNotNull(response.getNode());
        assertNull(response.getNode().getValue());
        assertNotNull(response.getPrevNode());
        assertEquals("Hello etcd", response.getPrevNode().getValue());
    }
}