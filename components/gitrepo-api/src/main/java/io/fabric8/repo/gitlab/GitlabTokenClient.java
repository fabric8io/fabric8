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

import io.fabric8.utils.cxf.WebClients;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;

import java.util.List;

import static io.fabric8.utils.cxf.WebClients.configureAuthorization;
import static io.fabric8.utils.cxf.WebClients.disableSslChecks;

/**
 * A client API for working with git hosted repositories using back ends like
 * <a href="http://gogs.io/">gogs</a> or <a href="http://github.com/">github</a>
 */
public class GitlabTokenClient extends GitlabClientSupport {

    private String authorizationType;
    private String authorization;

    public GitlabTokenClient(String address, String username, String authorizationType, String authorization) {
        super(address, username);
        this.authorizationType = authorizationType;
        this.authorization = authorization;
    }


    /**
     * Creates a JAXRS web client for the given JAXRS client
     */
    @Override
    protected <T> T createWebClient(Class<T> clientType) {
        List<Object> providers = WebClients.createProviders();
        WebClient webClient = WebClient.create(address, providers);
        disableSslChecks(webClient);
        configureAuthorization(webClient, username, authorizationType, authorization);
        return JAXRSClientFactory.fromClient(webClient, clientType);
    }

}
