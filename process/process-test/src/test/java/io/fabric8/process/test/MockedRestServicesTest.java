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
package io.fabric8.process.test;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import static io.fabric8.process.test.SystemPropertiesMockRestServiceDirectory.serviceUrlProperty;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class MockedRestServicesTest extends Assert {

    MockedRestServices restServices = new MockedRestServices();

    @After
    public void after() {
        restServices.stop();
    }

    @Test
    public void shouldConfigureWithCallback() throws IOException {
        // Given
        restServices.symbol("fooService").configure().
                when(request()).respond(response().withBody("Hello world!"));

        // When
        String url = System.getProperty(serviceUrlProperty("fooService"));
        String response = IOUtils.toString(new URL(url));

        // Then
        assertEquals("Hello world!", response);
    }

    @Test
    public void shouldReturnJson() throws IOException {
        // Given
        restServices.symbol("fooService").json(new int[]{1, 2, 3});

        // When
        String url = System.getProperty(serviceUrlProperty("fooService"));
        String response = IOUtils.toString(new URL(url));

        // Then
        assertEquals("[1,2,3]", response);
    }

    @Test
    public void shouldReturnJsonPerPath() throws IOException {
        // Given
        restServices.symbol("fooService").
                json("one", 1).
                json("two", 2);

        // When
        String url = System.getProperty(serviceUrlProperty("fooService"));
        String responseForOne = IOUtils.toString(new URL(url + "/api/one"));
        String responseForTwo = IOUtils.toString(new URL(url + "/api/two"));

        // Then
        assertEquals("1", responseForOne);
        assertEquals("2", responseForTwo);
    }

    @Test
    public void shouldReturnNonDefaultJson() throws IOException {
        // Given
        restServices.symbol("fooService").
                json("nonDefault", "nonDefault").
                json("default");

        // When
        String url = System.getProperty(serviceUrlProperty("fooService"));
        String responseForOne = IOUtils.toString(new URL(url + "/default"));
        String responseForTwo = IOUtils.toString(new URL(url + "/nonDefault"));

        // Then
        assertEquals("\"default\"", responseForOne);
        assertEquals("\"nonDefault\"", responseForTwo);
    }

}