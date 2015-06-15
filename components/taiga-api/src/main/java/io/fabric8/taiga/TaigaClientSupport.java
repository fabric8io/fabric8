/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.taiga;

import javax.xml.stream.events.Namespace;
import java.util.List;
import java.util.concurrent.Callable;

import static io.fabric8.utils.cxf.WebClients.handle404ByReturningNull;

/**
 */
public abstract class TaigaClientSupport {
    protected final String address;
    protected final String username;
    protected final String password;
    private TaigaApi api;
    private AuthDetailDTO authentication;

    public TaigaClientSupport(String address, String username, String password) {
        this.address = address;
        this.password = password;
        this.username = username;
    }

    public ProjectDTO createProject(ProjectDTO dto) {
        return getApi().createProject(dto);
    }

    public List<ProjectDTO> getProjects() {
        return getApi().getProjects();
    }

    public ProjectDTO getProjectById(final String id) {
        return handle404ByReturningNull(new Callable<ProjectDTO>() {
            @Override
            public ProjectDTO call() throws Exception {
                return getApi().getProjectById(id);
            }
        });
    }

    public ProjectDTO getProjectBySlug(final String slug) {
        return handle404ByReturningNull(new Callable<ProjectDTO>() {
            @Override
            public ProjectDTO call() throws Exception {
                return getApi().getProjectBySlug(slug);
            }
        });
    }

    protected TaigaApi getApi() {
        if (api == null) {
            api = createWebClient(TaigaApi.class);
            doAuthentication(api);
        }
        return api;
    }

    protected void doAuthentication(TaigaApi api) {
        AuthDTO authDto = new AuthDTO();
        authDto.setUsername(username);
        authDto.setPassword(password);
        authentication = getApi().authenticate(authDto);
        System.out.println("Got authentication: " + authentication);
    }

    public String getAddress() {
        return address;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    protected abstract <T> T createWebClient(Class<T> clientType);

    protected String getAuthToken() {
        if (authentication != null) {
            return authentication.getAuthToken();
        }
        return null;
    }
}
