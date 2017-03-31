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
package io.fabric8.taiga;

import io.fabric8.utils.Strings;
import io.fabric8.utils.URLUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static io.fabric8.taiga.Projects.addUser;
import static io.fabric8.utils.jaxrs.JAXRSClients.handle404ByReturningNull;

/**
 * Default base class for a TaigaClient implementation
 */
public abstract class TaigaClientSupport {
    protected final String address;
    protected final String username;
    protected final String password;
    private TaigaApi api;
    private AuthDetailDTO authentication;
    private boolean autoCreateProjects = true;

    public TaigaClientSupport(String address, String username, String password) {
        this.address = address;
        this.password = password;
        this.username = username;
    }

    /**
     * Returns the project ID for the given slug or null if none could be found
     */
    public Long getProjectIdForSlug(String slug) {
        ProjectDTO project = getProjectBySlug(slug);
        return project != null ? project.getId() : null;
    }

    /**
     * For the given project name try find the project by its slug or create a new project
     */
    public ProjectDTO getOrCreateProject(String name) {
        return getOrCreateProject(name, null);
    }

    /**
     * Find the project from the given name and optional slug; or create one if its not already existing
     */
    public ProjectDTO getOrCreateProject(final String name, final String slugOrNull) {
        String slug = validateSlug(slugOrNull, name);
        ProjectDTO project = getProjectBySlug(slug);
        if (project == null) {
            project = new ProjectDTO();
            project.setSlug(slug);
            project.setName(name);
            project.setDescription("Description of project " + name);

            // lets default a user
            addUser(project, getMe());
            return createProject(project);
        } else {
            return project;
        }
    }

    public ModuleDTO moduleForProject(String slug, TaigaModule module) {
        return moduleForProject(slug, module.toModuleKey());
    }

    public ModuleDTO moduleForProject(String slug, String module) {
        Map<String, ModuleDTO> map = getModulesForProject(slug);
        return map.get(module);
    }

    public ModuleDTO moduleForProject(Long projectId, TaigaModule module) {
        return moduleForProject(projectId, module.toModuleKey());
    }

    public ModuleDTO moduleForProject(Long projectId, String module) {
        Map<String, ModuleDTO> map = getModulesForProject(projectId);
        return map.get(module);
    }

    /**
     * Returns the webhook URL for the given module.
     *
     * The module webhook might not use the correct public host name so lets convert it.
     */
    public String getPublicWebhookUrl(ModuleDTO module) {
        if (module != null) {
            String webhooksUrl = module.getWebhooksUrl();
            if (Strings.isNotBlank(webhooksUrl)) {
                int idx = webhooksUrl.indexOf("/api/v");
                if (idx > 0) {
                    return URLUtils.pathJoin(getAddress(), webhooksUrl.substring(idx));
                }
            }
            return webhooksUrl;

        }
        return null;
    }

    // Delegate of TaigaApi
    //-------------------------------------------------------------------------

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

    public UserDTO getMe() {
        return handle404ByReturningNull(new Callable<UserDTO>() {
            @Override
            public UserDTO call() throws Exception {
                return getApi().getMe();
            }
        });
    }

    public UserDTO getUser(final String id) {
        return handle404ByReturningNull(new Callable<UserDTO>() {
            @Override
            public UserDTO call() throws Exception {
                return getApi().getUser(id);
            }
        });
    }

    public Map<String, ModuleDTO> getModulesForProject(String slug) {
        Long id = getProjectIdForSlug(slug);
        if (id != null) {
            return getModulesForProject(id);
        } else {
            return new HashMap<>();
        }
    }

    public Map<String, ModuleDTO> getModulesForProject(final Long id) {
        Map<String, ModuleDTO> answer = handle404ByReturningNull(new Callable<Map<String, ModuleDTO>>() {
            @Override
            public Map<String, ModuleDTO> call() throws Exception {
                return getApi().getModulesForProject(id);
            }
        });
        if (answer == null) {
            answer = new HashMap<>();
        }
        return answer;
    }


    // Properties
    //-------------------------------------------------------------------------

    public String getAddress() {
        return address;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public boolean isAutoCreateProjects() {
        return autoCreateProjects;
    }

    public void setAutoCreateProjects(boolean autoCreateProjects) {
        this.autoCreateProjects = autoCreateProjects;
    }

    // Implementation
    //-------------------------------------------------------------------------
    protected abstract <T> T createWebClient(Class<T> clientType);

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
    }

    protected String getAuthToken() {
        if (authentication != null) {
            return authentication.getAuthToken();
        }
        return null;
    }

    /**
     * If a slug is not supplied then lets generate it from the project name
     */
    protected String validateSlug(String slug, String name) {
        if (Strings.isNotBlank(slug)) {
            return slug;
        } else {
            return getUsername() + "-" + name;
        }
    }

}
