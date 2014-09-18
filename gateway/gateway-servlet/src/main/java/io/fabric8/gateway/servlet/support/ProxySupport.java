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

import org.apache.commons.httpclient.Header;

import javax.servlet.http.HttpServletResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for Proxy related operations.
 */
public final class ProxySupport {

    public static final String JSESSIONID = "JSESSIONID";
    private static final Pattern PATH_AND_DOMAIN_PATTERN = Pattern.compile("(?:;\\s*([pP]ath|[dD]omain)=([^;\\s]+))");

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

    /**
     * Returns the contents of a {@code Set-Cookie} header with it's optional {@code path} and
     * {@code domain} attributes replaced by the passed in values.
     *
     * @param header the {@code Set-Cookie} header for which {@code path} and {@code domain} should be replaced.
     * @param path the new path for the cookie. If null the original value of for path is used.
     * @param domain the domain for the cookie. If null the original value of for domain is used.
     * @return {@code String} the contents of the {@code Set-Cookie} header. The reason for not returning the complete
     *                        header including the headers name (Set-Cookie) is make this method useful with the
     *                        Java Servlet API and {@link HttpServletResponse#setHeader(String, String)}.
     */
    public static String replaceCookieAttributes(final String header, final String path, final String domain) {
        final Matcher m = PATH_AND_DOMAIN_PATTERN.matcher(header);
        final StringBuffer sb = new StringBuffer();
        while (m.find()) {
            final String name = m.group(1);
            if ("domain".equalsIgnoreCase(name)) {
                appendReplacement(m, sb, name, domain);
            } else if ("path".equalsIgnoreCase(name)) {
                appendReplacement(m, sb, name, path);
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static void appendReplacement(final Matcher m, final StringBuffer sb, final String name, final String value) {
        m.appendReplacement(sb, ';' + name + '=' + (value == null ? m.group(2) : value));
    }

    /**
     * Determines whether the passed-in header is a 'Set-Cookie' header.
     *
     * @param header the {@link Header} to check
     * @return {@code true} if the header is a 'Set-Cookie' header, false otherwise.
     */
    public static boolean isSetCookieHeader(final Header header) {
        return header.getName().equalsIgnoreCase("Set-Cookie");
    }
}
