/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.repo.git;

import javax.ws.rs.WebApplicationException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Callable;

import static io.fabric8.utils.jaxrs.JAXRSClients.handle404ByReturningNull;

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

    public GitRepoClientSupport(String address, String username) {
        this.username = username;
        this.address = address;
        this.password = null;
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

    public RepositoryDTO getRepository(final String owner, final String repo) {
        return handle404ByReturningNull(new Callable<RepositoryDTO>() {
            @Override
            public RepositoryDTO call() throws Exception {
                return getApi().getRepository(owner, repo);
            }
        });
    }

    public RepositoryDTO getOrganisationRepository(final String organisation, final String repo) {
        return handle404ByReturningNull(new Callable<RepositoryDTO>() {
            @Override
            public RepositoryDTO call() throws Exception {
                return getApi().getOrganisationRepository(organisation, repo);
            }
        });
    }

    public InputStream getRawFile(String username, String repo, String branch, String path) {
        try {
            return getApi().getRawFile(username, repo, branch, path);
        } catch (WebApplicationException e) {
            int status = e.getResponse().getStatus();
            // for some reason gogs returns a 500 rather than 404
            if (status == 500 || status == 404) {
                return null;
            } else {
                throw e;
            }
        }
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
