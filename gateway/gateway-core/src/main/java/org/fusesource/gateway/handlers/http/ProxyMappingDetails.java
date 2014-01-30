/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.gateway.handlers.http;

/**
 * The details of the mapping from the front end gateway to the back end service implementation so that
 * we can do things like rewrite reponse headers
 */
public class ProxyMappingDetails {
    private final String proxyServiceUrl;
    private final String reverseServiceUrl;
    private final String servicePath;

    public ProxyMappingDetails(String proxyServiceUrl, String reverseServiceUrl, String servicePath) {
        this.proxyServiceUrl = proxyServiceUrl;
        this.reverseServiceUrl = reverseServiceUrl;
        this.servicePath = servicePath;
    }

    @Override
    public String toString() {
        return "ProxyMappingDetails{" +
                "proxyServiceUrl='" + proxyServiceUrl + '\'' +
                ", reverseServiceUrl='" + reverseServiceUrl + '\'' +
                ", servicePath='" + servicePath + '\'' +
                '}';
    }

    /**
     * If a URL is relative to the back end proxy service then redirect it to be relative to the front end service
     *
     * @return the rewritten URL or the original if it does not start with the {@link #getProxyServiceUrl()}
     */
    public String rewriteBackendUrl(String backEndUrl) {
        if (backEndUrl != null && backEndUrl.startsWith(proxyServiceUrl)) {
            String remaining = backEndUrl.substring(proxyServiceUrl.length());
            if (reverseServiceUrl.endsWith("/") && remaining.startsWith("/")) {
                remaining = remaining.substring(1);
            }
            return reverseServiceUrl + remaining;
        }
        return backEndUrl;
    }

    /**
     * Returns the base URL of the back end service being invoked including the exposed path of the service.
     */
    public String getProxyServiceUrl() {
        return proxyServiceUrl;
    }

    /**
     * Returns the base URL of the front end request including the matched path.
     */
    public String getReverseServiceUrl() {
        return reverseServiceUrl;
    }

    /**
     * The full path being invoked on the back end service
     */
    public String getServicePath() {
        return servicePath;
    }
}
