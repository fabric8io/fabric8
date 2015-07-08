/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.repo.git.resteasy;

import io.fabric8.repo.git.GitRepoClientSupport;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.plugins.providers.DefaultTextPlain;
import org.jboss.resteasy.plugins.providers.FileProvider;
import org.jboss.resteasy.plugins.providers.InputStreamProvider;
import org.jboss.resteasy.plugins.providers.StringTextStar;
import org.jboss.resteasy.plugins.providers.jackson.Jackson2JsonpInterceptor;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * A client API for working with git hosted repositories using back ends like
 * <a href="http://gogs.io/">gogs</a> or <a href="http://github.com/">github</a>
 */
public class ResteasyGitRepoClient extends GitRepoClientSupport {

    /**
     * When used in the Jenkins Job DSL we may not have the right class loader setup.
     *
     * So lets set the context class loader first to ensure things are OK
     */
    public static ResteasyGitRepoClient createWithContextClassLoader(String address, String username, String password) {
        Thread.currentThread().setContextClassLoader(ResteasyGitRepoClient.class.getClassLoader());
        return new ResteasyGitRepoClient(address, username, password);

    }
    public ResteasyGitRepoClient(String address, String username, String password) {
        super(address, username, password);
    }

    /**
     * Creates a JAXRS web client for the given JAXRS client
     */
    protected <T> T createWebClient(Class<T> clientType) {
        String address = getAddress();

        ResteasyProviderFactory providerFactory = ResteasyProviderFactory.getInstance();
        providerFactory.register(ResteasyJackson2Provider.class);
        providerFactory.register(Jackson2JsonpInterceptor.class);
        providerFactory.register(StringTextStar.class);
        providerFactory.register(DefaultTextPlain.class);
        providerFactory.register(FileProvider.class);
        providerFactory.register(InputStreamProvider.class);
        providerFactory.register(new Authenticator());
        providerFactory.register(clientType);

        ResteasyClientBuilder builder = new ResteasyClientBuilder();
        builder.providerFactory(providerFactory);
        builder.connectionPoolSize(3);

        Client client = builder.build();
        ResteasyWebTarget target = (ResteasyWebTarget) client.target(address);
        return target.proxy(clientType);
    }


    protected class Authenticator implements ClientRequestFilter {

        public void filter(ClientRequestContext requestContext) throws IOException {
            MultivaluedMap<String, Object> headers = requestContext.getHeaders();
            final String basicAuthentication = getBasicAuthentication();
            headers.add("Authorization", basicAuthentication);
        }

        private String getBasicAuthentication() {
            String token = getUsername() + ":" + getPassword();
            try {
                return "Basic " +
                     DatatypeConverter.printBase64Binary(token.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                throw new IllegalStateException("Cannot encode with UTF-8", ex);
            }
        }
    }
}
