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
package org.fusesource.gateway.fabric.haproxy.model;

import java.net.URL;

/**
 * Represents a back end server in a haproxy config
 */
public class BackEndServer {
    private final URL url;

    public BackEndServer(URL url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "BackEndServer{" +
                "url=" + url +
                '}';
    }

    public URL getUrl() {
        return url;
    }

    public String getHost() {
        return url.getHost();
    }

    public String getProtocol() {
        return url.getProtocol();
    }

    public int getPort() {
        int answer = url.getPort();
        if (answer == 0) {
            answer = 80;
        }
        return answer;
    }
}
