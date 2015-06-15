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
package io.fabric8.repo.git;

import javax.ws.rs.PathParam;
import java.util.List;

/**
 */
public abstract class GitRepoClientSupport {
    protected final String address;
    protected final String username;
    protected final String password;
    private GitApi api;

    public GitRepoClientSupport(String address, String username, String password) {
        this.address = address;
        this.password = password;
        this.username = username;
    }

    public List<RepositoryDTO> listRepositories() {
        return getApi().listRepositories();
    }

    public List<RepositoryDTO> listOrganisationRepositories(String organisation) {
        return getApi().listOrganisationRepositories(organisation);
    }

    public List<OrganisationDTO> listUserOrganisations() {
        return getApi().listUserOrganisations();
    }

    public WebHookDTO createWebhook(String owner, String repo, CreateWebhookDTO dto) {
        return getApi().createWebhook(owner, repo, dto);
    }

    public List<WebHookDTO> getWebhooks(String owner, String repo) {
        return getApi().getWebhooks(owner, repo);
    }

    public RepositoryDTO createRepository(CreateRepositoryDTO createRepository) {
        return getApi().createRepository(createRepository);
    }


    protected GitApi getApi() {
        if (api == null) {
            api = createWebClient(GitApi.class);
        }
        return api;
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
}
