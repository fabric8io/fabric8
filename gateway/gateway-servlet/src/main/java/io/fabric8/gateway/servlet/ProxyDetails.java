/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.gateway.servlet;

import io.fabric8.common.util.Strings;
import io.fabric8.gateway.model.HttpProxyRule;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;

/**
 */
public class ProxyDetails {
    private final boolean valid;
    private final String stringProxyURL;

    private final HttpProxyRule proxyRule;
    private String proxyHostAndPort;
    private String proxyPath;

    public ProxyDetails(boolean valid, String stringProxyURL, HttpProxyRule proxyRule) {
        this.valid = valid;
        this.stringProxyURL = stringProxyURL;
        this.proxyRule = proxyRule;
        if (proxyHostAndPort == null) {
            return;
        }
        while (proxyHostAndPort.startsWith("/")) {
            proxyHostAndPort = proxyHostAndPort.substring(1);
        }
        int port = 0;
        String host = proxyHostAndPort;
        int idx = indexOf(proxyHostAndPort, ":", "/");
        if (idx > 0) {
            host = proxyHostAndPort.substring(0, idx);
            String portText = proxyHostAndPort.substring(idx + 1);
            idx = portText.indexOf("/");
            if (idx >= 0) {
                proxyPath = portText.substring(idx);
                portText = portText.substring(0, idx);
            }

            if (Strings.isNotBlank(portText)) {
                // portText may be a port unless its default
                try {
                    port = Integer.parseInt(portText);
                    proxyHostAndPort = host + ":" + port;
                } catch (NumberFormatException e) {
                    port = 80;
                    // we do not have a port, so proxyPath is the portText
                    proxyPath = "/" + portText + proxyPath;
                    proxyHostAndPort = host;
                }
            } else {
                proxyHostAndPort = host;
            }
        }
    }

    /**
     * Returns the lowest index of the given list of values
     */
    protected int indexOf(String text, String... values) {
        int answer = -1;
        for (String value : values) {
            int idx = text.indexOf(value);
            if (idx >= 0) {
                if (answer < 0 || idx < answer) {
                    answer = idx;
                }
            }
        }
        return answer;
    }

    public boolean isValid() {
        return valid;
    }

    public String getStringProxyURL() {
        return stringProxyURL;
    }

    public HttpClient createHttpClient(HttpMethod httpMethodProxyRequest) {
        HttpClient client = new HttpClient();
        return client;
    }

    public String getProxyHostAndPort() {
        return proxyHostAndPort;
    }

    public String getProxyPath() {
        return proxyPath;
    }

    public HttpProxyRule getProxyRule() {
        return proxyRule;
    }

}
