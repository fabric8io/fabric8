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
package io.fabric8.repo.gitlab;

import io.fabric8.utils.Strings;
import io.fabric8.utils.cxf.WebClients;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.List;

import static io.fabric8.utils.cxf.WebClients.configureUserAndPassword;
import static io.fabric8.utils.cxf.WebClients.createPrivateTokenFilter;
import static io.fabric8.utils.cxf.WebClients.disableSslChecks;

/**
 * A client API for working with gitlab
 */
public class GitlabClient extends GitlabClientSupport implements GitlabApi{

    public GitlabClient(String address, String username) {
        super(address, username);
    }

    public GitlabClient(String address, String username, String password) {
        super(address, username, password);
    }

    @GET
    @Path("namespaces")
    public List<NamespaceDTO> getNamespaces() {
        return getApi().getNamespaces();
    }

    @GET
    @Path("groups")
    public List<GroupDTO> getGroups() {
        return getApi().getGroups();
    }

    @POST
    @Path("group")
    public GroupDTO createGroup(CreateGroupDTO dto) {
        return getApi().createGroup(dto);
    }

    @GET
    @Path("groups/{groupId}/projects")
    public List<ProjectDTO> getProjects(Long projectId) {
        return getApi().getProjects(projectId);
    }

    @GET
    @Path("projects")
    public List<ProjectDTO> getProjects() {
        return getApi().getProjects();
    }

    @GET
    @Path("projects/{projectId}")
    public List<ProjectDTO> getProject(Long projectId) {
        return getApi().getProject(projectId);
    }

    @GET
    @Path("issues")
    public List<IssueDTO> getIssues() {
        return getApi().getIssues();
    }

    @GET
    @Path("groups/{groupId}/issues")
    public List<IssueDTO> getGroupIssues(Long groupId) {
        return getApi().getGroupIssues(groupId);
    }

    @GET
    @Path("projects/{projectId}/issues")
    public List<IssueDTO> getProjectIssue(Long projectId) {
        return getApi().getProjectIssue(projectId);
    }

    /**
     * Creates a JAXRS web client for the given JAXRS client
     */
    @Override
    protected <T> T createWebClient(Class<T> clientType) {
        List<Object> providers = WebClients.createProviders();
        if (Strings.isNotBlank(privateToken)) {
            providers.add(createPrivateTokenFilter(privateToken));
        }
        WebClient webClient = WebClient.create(address, providers);
        disableSslChecks(webClient);
        configureUserAndPassword(webClient, username, password);
        return JAXRSClientFactory.fromClient(webClient, clientType);
    }

}
