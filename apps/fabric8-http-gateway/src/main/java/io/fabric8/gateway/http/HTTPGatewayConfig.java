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
package io.fabric8.gateway.http;

import java.util.HashMap;

public class HTTPGatewayConfig extends HashMap<String, String> {

    private static final long serialVersionUID = -7563354951392959816L;
    /** The host name used when listening for HTTP traffic" */
    public final static String HOST = "HOST";
    /** Port number to listen on for HTTP requests") */
    public final static String PORT = "PORT";
    /** If enabled then performing a HTTP GET on the path '/' will return a JSON representation of the gateway mappings */
    public final static String ENABLE_INDEX = "ENABLE_INDEX";
    
    public int getPort() {
        return Integer.parseInt(get(PORT));
    }
    
    public String getHost() {
        return get(HOST);
    }
    
    public boolean isIndexEnabled() {
        return Boolean.parseBoolean(get(ENABLE_INDEX));
    }
}
