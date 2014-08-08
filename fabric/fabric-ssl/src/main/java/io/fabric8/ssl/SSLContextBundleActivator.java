/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.ssl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * A BundleActivator that replaces the JVM default SSLContext
 * if the System property javax.net.ssl.trustAll=true.
 *
 * The replaced SSL context will trust all presented SSL certificates
 * including self signed certs.
 *
 *
 * Created by chirino on 8/7/14.
 */
public class SSLContextBundleActivator implements BundleActivator {
    SSLContext original;

    @Override
    public void start(BundleContext bundleContext) throws Exception {

        String trustStore = System.getProperty("javax.net.ssl.trustStore");
        boolean trustAll = Boolean.getBoolean("javax.net.ssl.trustAll");

        boolean enabled = false;
        if( trustStore==null && trustAll ) {
            original = SSLContext.getDefault();
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(createKeyManagers(), trustAllCerts(), new java.security.SecureRandom());
            SSLContext.setDefault(ctx);
        } else if( trustStore!=null && trustAll ) {
            System.err.println();
            System.err.println("Invalid system property configuration:  The javax.net.ssl.trustStore and javax.net.ssl.trustAll cannot both be set.  Ignoring the javax.net.ssl.trustAll property");
            System.err.println();
        }
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        if( original!=null ) {
            SSLContext.setDefault(original);
            original = null;
        }
    }

    private KeyManager[] createKeyManagers() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        String keystore = System.getProperty("javax.net.ssl.keyStore");
        KeyManager[] keyManagers = null;
        if( keystore!=null ) {
            char[] password = System.getProperty("javax.net.ssl.keyStorePassword", "").toCharArray();
            String type = System.getProperty("javax.net.ssl.keyStoreType", "jks");

            KeyStore store = KeyStore.getInstance(type);
            store.load(new FileInputStream(keystore), password);

            KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            factory.init(store, password);
            keyManagers = factory.getKeyManagers();
        }
        return keyManagers;
    }

    private TrustManager[] trustAllCerts() {
        return new TrustManager[] {
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                public void checkClientTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
                    }
                public void checkServerTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };
    }
}
