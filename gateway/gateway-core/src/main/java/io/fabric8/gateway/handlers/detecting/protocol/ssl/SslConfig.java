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
package io.fabric8.gateway.handlers.detecting.protocol.ssl;

import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.security.KeyStore;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;


/**
 */
public class SslConfig {

    private URL keyStoreURL;
    private String keyStorePassword;
    private KeyStore keyStore;

    private URL trustStoreURL;
    private String trustStorePassword;
    private KeyStore trustStore;

    private String keyAlias;
    private String keyPassword;
    
    String storeType;
    String algorithm;

    TrustManager[] trustManagers;
    KeyManager[] keyManagers;
    private String protocol = "TLS";

    String disabledCypherSuites;
    String enabledCipherSuites;

    public SslConfig() {
    }

    public SslConfig(File keyStoreFile, String keyStorePassword) {
        this(url(keyStoreFile), keyStorePassword);
    }

    public SslConfig(File keyStoreFile, String keyStorePassword,  File trustStoreFile, String trustStorePassword) {
        this(url(keyStoreFile), keyStorePassword, url(trustStoreFile), trustStorePassword);
    }

    private static URL url(File fil) {
        if( fil == null ) {
            return null;
        }
        try {
            return fil.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public SslConfig(URL keyStoreURL, String keyStorePassword) {
        this(keyStoreURL, keyStorePassword, null, null);
    }

    public SslConfig(URL keyStoreURL, String keyStorePassword, URL trustStoreURL, String trustStorePassword) {
        this.keyStoreURL = keyStoreURL;
        this.keyStorePassword = keyStorePassword;
        this.trustStoreURL = trustStoreURL;
        this.trustStorePassword = trustStorePassword;
    }

    public KeyStore getKeyStore() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        if( keyStore==null ) {
            if (storeType == null) {
                storeType = "JKS";
            }
            if (keyStorePassword == null) {
                keyStorePassword = "";
            }
            KeyStore store = KeyStore.getInstance(storeType);
            InputStream stream = keyStoreURL.openStream();
            try {
                store.load(stream, keyStorePassword.toCharArray());
            } finally {
                stream.close();
            }
            keyStore = store;
        }
        return keyStore;

    }

    public KeyStore getTrustStore() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        if( trustStoreURL ==null ) {
            return getKeyStore();
        }
        if( trustStore==null ) {
            if (storeType == null) {
                storeType = "JKS";
            }
            if (trustStorePassword == null) {
                trustStorePassword = "";
            }
            KeyStore store = KeyStore.getInstance(storeType);
            InputStream stream = trustStoreURL.openStream();
            try {
                store.load(stream, trustStorePassword.toCharArray());
            } finally {
                stream.close();
            }
            trustStore = store;
        }
        return trustStore;
    }

    public TrustManager[] getTrustManagers() throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
        if( trustManagers == null ) {
            if( algorithm == null ) {
                algorithm = "SunX509";
            }
            TrustManagerFactory factory = TrustManagerFactory.getInstance(algorithm);
            factory.init(getTrustStore());
            trustManagers = factory.getTrustManagers();
        }
        return trustManagers;
    }

    public KeyManager[] getKeyManagers() throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException, UnrecoverableKeyException {
      if( keyManagers ==null ) {
          if( algorithm == null ) {
              algorithm = "SunX509";
          }
          if( keyPassword == null ) {
              keyPassword = "";
          }

        KeyManagerFactory factory = KeyManagerFactory.getInstance(algorithm);
        factory.init(getKeyStore(),keyPassword.toCharArray());
        keyManagers = factory.getKeyManagers();

        if( keyAlias!=null ) {
            for (int i = 0; i < keyManagers.length; i++) {
                KeyManager keyManager = keyManagers[i];
                if( keyManager instanceof X509ExtendedKeyManager) {
                    keyManagers[i] = new AliasFilteringKeyManager(keyAlias, (X509ExtendedKeyManager) keyManager);
                }
            }
        }
      }
      return keyManagers;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    static class AliasFilteringKeyManager extends X509ExtendedKeyManager {
        private final String alias;
        private final X509ExtendedKeyManager next;

        AliasFilteringKeyManager(String alias, X509ExtendedKeyManager next) {
            this.alias = alias;
            this.next = next;
        }

        @Override
        public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
            return alias;
        }

        @Override
        public String chooseServerAlias(String s, Principal[] principals, Socket socket) {
            return alias;
        }

        @Override
        public String chooseEngineClientAlias(String[] strings, Principal[] principals, SSLEngine sslEngine) {
            return alias;
        }

        @Override
        public String chooseEngineServerAlias(String s, Principal[] principals, SSLEngine sslEngine) {
            return alias;
        }

        @Override
        public X509Certificate[] getCertificateChain(String s) {
            return next.getCertificateChain(s);
        }

        @Override
        public String[] getClientAliases(String s, Principal[] principals) {
            return next.getClientAliases(s, principals);
        }

        @Override
        public PrivateKey getPrivateKey(String s) {
            return next.getPrivateKey(s);
        }

        @Override
        public String[] getServerAliases(String s, Principal[] principals) {
            return next.getServerAliases(s, principals);
        }
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getStoreType() {
        return storeType;
    }

    public void setStoreType(String storeType) {
        this.storeType = storeType;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public URL getKeyStoreURL() {
        return keyStoreURL;
    }

    public void setKeyStoreURL(URL keyStoreURL) {
        this.keyStoreURL = keyStoreURL;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public URL getTrustStoreURL() {
        return trustStoreURL;
    }

    public void setTrustStoreURL(URL trustStoreURL) {
        this.trustStoreURL = trustStoreURL;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public String getDisabledCypherSuites() {
        return disabledCypherSuites;
    }

    public void setDisabledCypherSuites(String disabledCypherSuites) {
        this.disabledCypherSuites = disabledCypherSuites;
    }

    public String getEnabledCipherSuites() {
        return enabledCipherSuites;
    }

    public void setEnabledCipherSuites(String enabledCipherSuites) {
        this.enabledCipherSuites = enabledCipherSuites;
    }

}
