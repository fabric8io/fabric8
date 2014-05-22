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
package io.fabric8.gateway.servlet.support;

/**
 * Utility methods for Proxy related operations.
 */
public final class ProxySupport {

    private ProxySupport() {
    }

    /**
     * Checks if the passed-in header is a hop-by-hop header as defined by
     * <a href="http://tools.ietf.org/html/rfc2616#section-13.5.1">RFC-2616 Section 13.5.1</a>.
     *
     * @param header the header name to check.
     * @return {@code true} if the header is a hop-by-hop header, false otherwise.
     */
    public static boolean isHopByHopHeader(final String header){
        return  header.equalsIgnoreCase("Connection") ||
                header.equalsIgnoreCase("Keep-Alive") ||
                header.equalsIgnoreCase("Proxy-Authentication") ||
                header.equalsIgnoreCase("Proxy-Authorization") ||
                header.equalsIgnoreCase("TE") ||
                header.equalsIgnoreCase("Trailers") ||
                header.equalsIgnoreCase("Transfer-Encoding") ||
                header.equalsIgnoreCase("Upgrade");
    }
}
