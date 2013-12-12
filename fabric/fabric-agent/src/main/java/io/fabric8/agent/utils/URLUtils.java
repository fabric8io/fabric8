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
package io.fabric8.agent.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import io.fabric8.utils.Base64Encoder;

/**
 * Url related utility methods.
 *
 * @author Niclas Heldman
 * @author Alin Dreghiciu
 * @since 0.5.0, January 16, 2008
 */
public class URLUtils {

    /**
     * Utility class. Ment to be used via static methods.
     */
    private URLUtils() {
        // utility class
    }

    /**
     * Prepares an url connection for authentication if necessary.
     *
     * @param connection the connection to be prepared
     *
     * @return the prepared conection
     */
    public static URLConnection prepareForAuthentication(final URLConnection connection) {
        NullArgumentException.validateNotNull(connection, "url connection cannot be null");
        if (connection.getURL().getUserInfo() != null) {
            // Need to decode username/password because it may contain encoded
            // characters (http://www.w3schools.com/tags/ref_urlencode.asp)
            // A common encoding is to provide a username as an email address
            // like user%40domain.org
            String decodedUserInfo = decode(connection.getURL().getUserInfo());

            String base64Encoded = Base64Encoder.encode(decodedUserInfo);
            // sun bug 6459815: Long passwords cause Basic Auth to fail with a
            // java.net.Authenticator
            base64Encoded = base64Encoded.replaceAll("\n", "");
            connection.setRequestProperty("Authorization", "Basic " + base64Encoded);
        }
        return connection;
    }

    /**
     * Decodes the specified (portion of a) URL. <strong>Note:</strong> This decoder assumes that ISO-8859-1 is used to
     * convert URL-encoded octets to characters.
     * 
     * @param url The URL to decode, may be <code>null</code>.
     * @return The decoded URL or <code>null</code> if the input was <code>null</code>.
     */
    public static String decode(String url) {
        if (url == null) {
            return null;
        }
        StringBuilder decoded = new StringBuilder();
        int pos = 0;
        while (pos < url.length()) {
            char ch = url.charAt(pos);
            if (ch == '%') {
                if (pos + 2 < url.length()) {
                    String hexStr = url.substring(pos + 1, pos + 3);
                    char hexChar = (char) Integer.parseInt(hexStr, 16);
                    decoded.append(hexChar);
                    pos += 3;
                } else {
                    throw new IllegalStateException("'%' escape must be followed by two hex digits");
                }
            } else {
                decoded.append(ch);
                pos++;
            }
        }
        return decoded.toString();
    }

    /**
     * Prepares an url connection for authentication if necessary.
     *
     * @param connection the connection to be prepared
     * @return the prepared conection
     */
    public static URLConnection prepareForSSL(final URLConnection connection) {
        NullArgumentException.validateNotNull(connection, "url connection cannot be null");
        URLConnection conn = connection;
        if (conn instanceof JarURLConnection) {
            try {
                conn = ((JarURLConnection) connection).getJarFileURL().openConnection();
                conn.connect();
            } catch (IOException e) {
                throw new RuntimeException("Could not prepare connection for HTTPS.", e);
            }
        }
        if (conn instanceof HttpsURLConnection) {
            try {
                SSLContext ctx = SSLContext.getInstance("SSLv3");
                ctx.init(null, new TrustManager[]{new AllCertificatesTrustManager()}, null);
                ((HttpsURLConnection) conn).setSSLSocketFactory(ctx.getSocketFactory());
            } catch (KeyManagementException e) {
                throw new RuntimeException("Could not prepare connection for HTTPS.", e);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Could not prepare connection for HTTPS.", e);
            }

        }
        return connection;
    }

    /**
     * Prepare url for authentication and ssl if necessary and returns the input stream from the url.
     *
     * @param url                  url to prepare
     * @param acceptAnyCertificate true if the certicate check should be skipped
     * @return input stream from url
     * @throws IOException re-thrown
     */
    public static InputStream prepareInputStream(final URL url, final boolean acceptAnyCertificate)
            throws IOException {
        final URLConnection conn = url.openConnection();
        prepareForAuthentication(conn);
        if (acceptAnyCertificate) {
            prepareForSSL(conn);
        }
        return conn.getInputStream();
    }

}
