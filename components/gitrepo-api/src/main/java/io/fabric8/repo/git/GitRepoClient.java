/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.repo.git;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.cfg.Annotations;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import io.fabric8.utils.Strings;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;

import java.util.ArrayList;
import java.util.List;

import static io.fabric8.repo.git.JsonHelper.createObjectMapper;

/**
 * A client API for working with git hosted repositories using back ends like
 * <a href="http://gogs.io/">gogs</a> or <a href="http://github.com/">github</a>
 */
public class GitRepoClient {
    private final String address;
    private final String username;
    private final String password;
    private GitApi api;

    public GitRepoClient(String address, String username, String password) {
        this.address = address;
        this.username = username;
        this.password = password;
    }

    public RepositoryDTO createRepository(CreateRepositoryDTO createRepository) {
        return getApi().createRepository(createRepository);
    }

    public List<RepositoryDTO> listRepositories() {
        return getApi().listRepositories();
    }

    protected GitApi getApi() {
        if (api == null) {
            api = createWebClient(GitApi.class);
        }
        return api;
    }

    /**
     * Creates a JAXRS web client for the given JAXRS client
     */
    protected <T> T createWebClient(Class<T> clientType) {
        List<Object> providers = createProviders();
        WebClient webClient = WebClient.create(address, providers);
        if (Strings.isNotBlank(username) && Strings.isNotBlank(password)) {
            HTTPConduit conduit = WebClient.getConfig(webClient).getHttpConduit();
            conduit.getAuthorization().setUserName(username);
            conduit.getAuthorization().setPassword(password);
        }
        return JAXRSClientFactory.fromClient(webClient, clientType);
    }

    protected List<Object> createProviders() {
        List<Object> providers = new ArrayList<Object>();
        Annotations[] annotationsToUse = JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS;
        ObjectMapper objectMapper = createObjectMapper();
        providers.add(new JacksonJaxbJsonProvider(objectMapper, annotationsToUse));
        providers.add(new ExceptionResponseMapper());
        return providers;
    }

}
