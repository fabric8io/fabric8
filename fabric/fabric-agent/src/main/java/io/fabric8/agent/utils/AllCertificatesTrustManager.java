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

import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

/**
 * A trust manager that will accept any certificate. I.e. thisclass performs NO TRUST MANAGEMENT and simply serves as
 * a mechanism through which https connections can be established with the same notion of trust as a http connection
 * (i.e. none).
 *
 * @author Niclas Hedhman
 * @author Alin Dreghiciu
 * @since 0.5.0, January 16, 2008
 */
public final class AllCertificatesTrustManager
        implements X509TrustManager {

    /**
     * Empty certificate sequence.
     */
    private static final X509Certificate[] EMPTY_CERTS = new X509Certificate[0];

    /**
     * Null implementation.
     *
     * @param certs    the supplied certs (ignored)
     * @param authType the supplied type (ignored)
     */
    public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
    }

    /**
     * Null implementation.
     *
     * @param certs    the supplied certs (ignored)
     * @param authType the supplied type (ignored)
     */
    public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
    }

    /**
     * Null implementation.
     *
     * @return an empty certificate array
     */
    public X509Certificate[] getAcceptedIssuers() {
        return EMPTY_CERTS;
    }
}
