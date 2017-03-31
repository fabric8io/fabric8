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
package io.fabric8.utils.jaxrs;

import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.Collections;

/**
 * Adds an authorization header to requests
 */
public class AuthorizationHeaderFilter implements ClientRequestFilter {
    private static final transient Logger LOG = LoggerFactory.getLogger(AuthorizationHeaderFilter.class);

    private String authorizationHeader;

    public String getAuthorizationHeader() {
        return authorizationHeader;
    }

    public void setAuthorizationHeader(String authorizationHeader) {
        this.authorizationHeader = authorizationHeader;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        if (Strings.isNotBlank(authorizationHeader)) {
            MultivaluedMap<String, Object> headers = requestContext.getHeaders();
            headers.put("Authorization", Collections.<Object>singletonList(authorizationHeader));
            if (LOG.isDebugEnabled()) {
                LOG.debug("Added authorizationHeader: " + authorizationHeader);
            }
        }
    }
}
