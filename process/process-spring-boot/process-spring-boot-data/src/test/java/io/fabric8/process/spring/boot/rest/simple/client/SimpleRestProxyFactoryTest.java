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
package io.fabric8.process.spring.boot.rest.simple.client;

import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestOperations;

import static io.fabric8.process.spring.boot.rest.simple.client.Header.header;
import static java.lang.String.format;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

public class SimpleRestProxyFactoryTest {

    RestOperations restOperations = mock(RestOperations.class);

    String baseServiceUri = "http://company.com/api/";

    RestProxy<FooService> fooServiceRestProxy = new SimpleRestProxyFactory(restOperations).
            proxyService(FooService.class, baseServiceUri);

    int arg = 666;

    @Test
    public void shouldGenerateGetRequest() {
        // When
        fooServiceRestProxy.get().getString(arg);

        // Then
        verify(restOperations).exchange(format("http://company.com/api/fooService/getString/%d", arg), GET, new HttpEntity<>(new HttpHeaders()), String.class);
    }

    @Test
    public void shouldGenerateGetRequestWithHeader() {
        // When
        fooServiceRestProxy.get(header("headerKey", "headerValue")).getString(arg);

        // Then
        HttpHeaders headers = new HttpHeaders();
        headers.set("headerKey", "headerValue");
        verify(restOperations).exchange(format("http://company.com/api/fooService/getString/%d", arg), GET, new HttpEntity<>(headers), String.class);
    }

    @Test
    public void shouldGeneratePostRequest() {
        // When
        fooServiceRestProxy.post().getString(arg);

        // Then
        verify(restOperations).exchange("http://company.com/api/fooService/getString", POST, new HttpEntity<>(arg), String.class);
    }

    @Test
    public void shouldGeneratePostRequestWithHeader() {
        // When
        fooServiceRestProxy.post(header("headerKey", "headerValue")).getString(arg);

        // Then
        HttpHeaders headers = new HttpHeaders();
        headers.set("headerKey", "headerValue");
        verify(restOperations).exchange("http://company.com/api/fooService/getString", POST, new HttpEntity<>(arg, headers), String.class);
    }

    @Test
    public void shouldRemoveExtraSlashFromBaseServiceUri() {
        // When
        fooServiceRestProxy.post().getString(arg);

        // Then
        verify(restOperations).exchange("http://company.com/api/fooService/getString", POST, new HttpEntity<>(arg), String.class);
    }

}