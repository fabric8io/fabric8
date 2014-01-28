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
package org.fusesource.gateway.fabric.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 */
public class UrlHelpers {
    private static final transient Logger LOG = LoggerFactory.getLogger(UrlHelpers.class);

    /**
     * Populates the parameters from the URL of the service so they can be reused in the URI template
     */
    public static void populateUrlParams(Map<String, String> params, String service) {
        try {
            URL url = new URL(service);
            params.put("contextPath", url.getPath());
            params.put("protocol", url.getProtocol());
            params.put("host", url.getHost());
            params.put("port", "" + url.getPort());

        } catch (MalformedURLException e) {
            LOG.warn("Invalid URL '" + service + "'. " + e);
        }
    }
}
