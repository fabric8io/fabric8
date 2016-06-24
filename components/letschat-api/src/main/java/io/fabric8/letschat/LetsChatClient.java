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
package io.fabric8.letschat;

import io.fabric8.utils.Strings;
import io.fabric8.utils.cxf.WebClients;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import static io.fabric8.utils.cxf.WebClients.disableSslChecks;

/**
 * A client API for working with git hosted repositories using back ends like
 * <a href="http://gogs.io/">gogs</a> or <a href="http://github.com/">github</a>
 */
public class LetsChatClient extends LetsChatClientSupport {

    public LetsChatClient(String address, String username, String password, String authToken) {
        super(address, username, password, authToken);
    }


    /**
     * Creates a JAXRS web client for the given JAXRS client
     */
    @Override
    protected <T> T createWebClient(Class<T> clientType) {
        List<Object> providers = WebClients.createProviders();
        providers.add(new Authenticator());
        WebClient webClient = WebClient.create(address, providers);
        disableSslChecks(webClient);
        return JAXRSClientFactory.fromClient(webClient, clientType);
    }


    protected class Authenticator implements ClientRequestFilter {

        public void filter(ClientRequestContext requestContext) throws IOException {
            MultivaluedMap<String, Object> headers = requestContext.getHeaders();
            String authenticationHeader = getAuthenticationHeader();
            if (Strings.isNotBlank(authenticationHeader)) {
                headers.add("Authorization", authenticationHeader);
            }
        }

        private String getAuthenticationHeader() {
            String token = getAuthToken();
            if (Strings.isNotBlank(token)) {
                return "Bearer " + token;
            } else {
                // lets use basic auth to generate the token?
                String userPassword = getUsername() + ":" + getPassword();
                try {
                    return "Basic " +
                            DatatypeConverter.printBase64Binary(userPassword.getBytes("UTF-8"));
                } catch (UnsupportedEncodingException ex) {
                    throw new IllegalStateException("Cannot encode with UTF-8", ex);
                }
            }
        }
    }

}
