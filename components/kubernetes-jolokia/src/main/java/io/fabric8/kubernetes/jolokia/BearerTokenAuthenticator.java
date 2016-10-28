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
package io.fabric8.kubernetes.jolokia;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.jolokia.client.J4pAuthenticator;

import java.io.IOException;

/**
 * Authenticator using the Bearer token.
 *
 * The token is expected to be provided in the pUser field of the {@link BearerTokenAuthenticator#authenticate(HttpClientBuilder, String, String)} method.
 */
public class BearerTokenAuthenticator implements J4pAuthenticator {

    public BearerTokenAuthenticator() {
    }

    /** {@inheritDoc} */
    public void authenticate(HttpClientBuilder pBuilder, String pUser, String pPassword) {
        pBuilder.addInterceptorFirst(new PreemptiveBearerInterceptor(pUser));
    }


    // =================================================================================================

    static class PreemptiveBearerInterceptor implements HttpRequestInterceptor {

        private String token;

        public PreemptiveBearerInterceptor(String token) {
            this.token = token;
        }

        /** {@inheritDoc} */
        public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
            request.addHeader("Authorization", "Bearer " + token);
        }
    }
}
