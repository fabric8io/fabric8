/*
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

package io.fabric8.docker.api;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.RecordedRequest;
import io.fabric8.docker.api.image.DeleteInfo;
import io.fabric8.docker.api.image.ImageHistoryItem;
import io.fabric8.docker.api.image.ImageInfo;
import io.fabric8.docker.api.image.ImageSearchResult;
import io.fabric8.docker.api.image.Progress;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static com.google.common.io.Resources.getResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ImageTest extends DockerBaseTest {

    @Test
    public void testListImages() throws IOException {
        String json = Resources.toString(getResource("image/images-all.json"), Charsets.UTF_8);
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(json));
        server.play();
        Docker docker = createDockerForMock(server);
        List<Image> images = docker.images(1);
        assertNotNull(images);
        assertEquals(images.size(), 5);
    }

    @Test
    public void testCreateImage() throws IOException {
        String json = Resources.toString(getResource("image/image-create.json"), Charsets.UTF_8);
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(json));
        server.play();
        Docker docker = createDockerForMock(server);
        Progress response = docker.imageCreate("base", null, null, null, null);
        assertNotNull(response);
        assertEquals(response.getId(), "b750fe79269d");
    }

    @Test
    public void testImageInsert() throws IOException {
        String json = Resources.toString(getResource("image/image-insert.json"), Charsets.UTF_8);
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(json));
        server.play();
        Docker docker = createDockerForMock(server);
        Progress response = docker.imageInsert("base", "somepath", "http://someurl");
        assertNotNull(response);
        assertEquals(response.getError(), "Invalid...");
    }


    @Test
    public void testImageInspect() throws IOException {
        String json = Resources.toString(getResource("image/image-inspect.json"), Charsets.UTF_8);
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(json));
        server.play();
        Docker docker = createDockerForMock(server);
        ImageInfo response = docker.imageInspect("b750fe79269d");
        assertNotNull(response);
        assertEquals(response.getId(), "b750fe79269d2ec9a3c593ef05b4332b1d1a02a62b4accb2c21d589ff2f5f2dc");
    }

    @Test
    public void testImageHistory() throws IOException {
        String json = Resources.toString(getResource("image/image-history.json"), Charsets.UTF_8);
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(json));
        server.play();
        Docker docker = createDockerForMock(server);
        List<ImageHistoryItem> history = docker.imageHistory("b750fe79269d");
        assertNotNull(history);
        assertEquals(history.size(), 2);
    }

    @Test
    public void testImagePush() throws IOException, InterruptedException {
        final String json = Resources.toString(getResource("image/image-push.json"), Charsets.UTF_8);
        final Auth auth = new Auth();
        auth.setUsername("hannibal");
        auth.setPassword("xxxx");
        auth.setEmail("hannibal@a-team.com");
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(json));
        server.play();
        Docker docker = createDockerForMock(server);
        Progress response = docker.imagePush("base", "reg", auth);
        assertNotNull(response);
        assertEquals(response.getError(), "Invalid...");
        RecordedRequest recordedRequest = server.takeRequest();
        String body = new String(recordedRequest.getBody());
        Auth requestAuth = JsonHelper.fromJson(body, Auth.class);
        assertEquals(auth.getEmail(), requestAuth.getEmail());
    }

    @Test
    public void testImageDelete() throws IOException {
        String json = Resources.toString(getResource("image/image-delete.json"), Charsets.UTF_8);
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(json));
        server.play();
        Docker docker = createDockerForMock(server);
        List<DeleteInfo> info = docker.imageDelete("b750fe79269d");
        assertNotNull(info);
        assertEquals(info.size(), 3);
    }


    @Test
    public void testImageSearch() throws IOException {
        String json = Resources.toString(getResource("image/image-search.json"), Charsets.UTF_8);
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(json));
        server.play();
        Docker docker = createDockerForMock(server);
        List<ImageSearchResult> results = docker.imageSearch("sshd");
        assertNotNull(results);
        assertEquals(results.size(), 3);
    }

}
