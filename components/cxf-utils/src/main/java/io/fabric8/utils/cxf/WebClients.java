/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.utils.cxf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.cfg.Annotations;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import io.fabric8.utils.Strings;
import io.fabric8.utils.ssl.TrustEverythingSSLTrustManager;
import net.oauth.signature.pem.PEMReader;
import net.oauth.signature.pem.PKCS1EncodedKeySpec;
import org.apache.cxf.attachment.Base64DecoderStream;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.auth.DigestAuthSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.fabric8.utils.jaxrs.JsonHelper.createObjectMapper;

/**
 */
public class WebClients {
    private static final transient Logger LOG = LoggerFactory.getLogger(WebClients.class);

    public static InputStream getInputStreamFromDataOrFile(String data, File file) throws FileNotFoundException {
        if (data != null) {
            return new Base64DecoderStream(new ByteArrayInputStream(data.getBytes()));
        }
        if (file != null) {
            return new FileInputStream(file);
        }
        return null;
    }

    public static KeyStore createTrustStore(String caCertData, File caCertFile) throws Exception {
        try (InputStream pemInputStream = getInputStreamFromDataOrFile(caCertData, caCertFile)) {
            CertificateFactory certFactory = CertificateFactory.getInstance("X509");
            X509Certificate cert = (X509Certificate) certFactory.generateCertificate(pemInputStream);

            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(null);

            String alias = cert.getSubjectX500Principal().getName();
            trustStore.setCertificateEntry(alias, cert);

            return trustStore;
        }
    }

    public static void configureCaCert(WebClient webClient, String caCertData, File caCertFile) {
        try {
            KeyStore trustStore = createTrustStore(caCertData, caCertFile);
            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

            HTTPConduit conduit = WebClient.getConfig(webClient)
                    .getHttpConduit();

            TLSClientParameters params = conduit.getTlsClientParameters();

            if (params == null) {
                params = new TLSClientParameters();
                conduit.setTlsClientParameters(params);
            }

            TrustManager[] existingTrustManagers = params.getTrustManagers();

            if (existingTrustManagers != null && existingTrustManagers.length > 0) {
                List<TrustManager> list = new ArrayList<>();
                list.addAll(Arrays.asList(existingTrustManagers));
                list.addAll(Arrays.asList(trustManagers));
                trustManagers = list.toArray(new TrustManager[list.size()]);
            }

            params.setTrustManagers(trustManagers);
        } catch (Exception e) {
            LOG.error("Could not create trust manager for " + caCertFile, e);
        }
    }

    public static void disableSslChecks(WebClient webClient) {
        HTTPConduit conduit = WebClient.getConfig(webClient)
                .getHttpConduit();

        TLSClientParameters params = conduit.getTlsClientParameters();

        if (params == null) {
            params = new TLSClientParameters();
            conduit.setTlsClientParameters(params);
        }

        params.setTrustManagers(new TrustManager[]{new TrustEverythingSSLTrustManager()});

        params.setDisableCNCheck(true);
    }

    public static void disableHostNameChecks(WebClient webClient) {
        HTTPConduit conduit = WebClient.getConfig(webClient)
                .getHttpConduit();

        TLSClientParameters params = conduit.getTlsClientParameters();

        if (params == null) {
            params = new TLSClientParameters();
            conduit.setTlsClientParameters(params);
        }
        LOG.debug("Disabling host name checks");

        params.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        });
    }

    public static void configureClientCert(WebClient webClient, String clientCertData, File clientCertFile, String clientKeyData, File clientKeyFile, String clientKeyAlgo, char[] clientKeyPassword) {
        try {
            KeyStore keyStore = createKeyStore(clientCertData, clientCertFile, clientKeyData, clientKeyFile, clientKeyAlgo, clientKeyPassword);
            KeyManagerFactory keyManagerFactory =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, clientKeyPassword);
            KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

            HTTPConduit conduit = WebClient.getConfig(webClient)
                    .getHttpConduit();

            TLSClientParameters params = conduit.getTlsClientParameters();

            if (params == null) {
                params = new TLSClientParameters();
                conduit.setTlsClientParameters(params);
            }

            KeyManager[] existingKeyManagers = params.getKeyManagers();

            if (existingKeyManagers != null && existingKeyManagers.length > 0) {
                List<KeyManager> list = new ArrayList<>();
                list.addAll(Arrays.asList(existingKeyManagers));
                list.addAll(Arrays.asList(keyManagers));
                keyManagers = list.toArray(new KeyManager[list.size()]);
            }

            params.setKeyManagers(keyManagers);

        } catch (Exception e) {
            LOG.error("Could not create key manager for " + clientCertFile + " (" + clientKeyFile + ")", e);
        }
    }

    public static KeyStore createKeyStore(String clientCertData, File clientCertFile, String clientKeyData, File clientKeyFile, String clientKeyAlgo, char[] clientKeyPassword) throws Exception {
        try (InputStream certInputStream = getInputStreamFromDataOrFile(clientCertData, clientCertFile)) {
            CertificateFactory certFactory = CertificateFactory.getInstance("X509");
            X509Certificate cert = (X509Certificate) certFactory.generateCertificate(certInputStream);

            InputStream keyInputStream = getInputStreamFromDataOrFile(clientKeyData, clientKeyFile);
            PEMReader reader = new PEMReader(keyInputStream);
            RSAPrivateCrtKeySpec keySpec = new PKCS1EncodedKeySpec(reader.getDerBytes()).getKeySpec();
            KeyFactory kf = KeyFactory.getInstance(clientKeyAlgo);
            RSAPrivateKey privKey = (RSAPrivateKey) kf.generatePrivate(keySpec);

            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, clientKeyPassword);

            String alias = cert.getSubjectX500Principal().getName();
            keyStore.setKeyEntry(alias, privKey, clientKeyPassword, new Certificate[] {cert});

            return keyStore;
        }
    }

    public static void configureUserAndPassword(WebClient webClient, String username, String password) {
        if (Strings.isNotBlank(username) && Strings.isNotBlank(password)) {
            HTTPConduit conduit = WebClient.getConfig(webClient).getHttpConduit();
            conduit.getAuthorization().setUserName(username);
            conduit.getAuthorization().setPassword(password);
        }
    }

    public static ClientRequestFilter createPrivateTokenFilter(final String privateToken) {
        ClientRequestFilter interceptor = new ClientRequestFilter() {

            @Override
            public void filter(ClientRequestContext requestContext) throws IOException {
                requestContext.getHeaders().add("PRIVATE-TOKEN", privateToken);
            }
        };
        return interceptor;
    }

    public static void configureAuthorization(WebClient webClient, String username, String authorizationType, String authorization) {
        HTTPConduit conduit = WebClient.getConfig(webClient).getHttpConduit();
        if (Strings.isNotBlank(username)) {
            conduit.getAuthorization().setUserName(username);
        }
        if (Strings.isNotBlank(authorizationType) && Strings.isNotBlank(authorization)) {
            conduit.getAuthorization().setUserName(username);
            conduit.getAuthorization().setAuthorizationType(authorizationType);
            conduit.getAuthorization().setAuthorization(authorization);
        }
    }

    public static void enableDigestAuthenticaionType(WebClient webClient) {
        HTTPConduit conduit = WebClient.getConfig(webClient).getHttpConduit();
        conduit.setAuthSupplier(new DigestAuthSupplier());
    }

    public static List<Object> createProviders() {
        List<Object> providers = new ArrayList<Object>();
        Annotations[] annotationsToUse = JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS;
        ObjectMapper objectMapper = createObjectMapper();
        providers.add(new JacksonJaxbJsonProvider(objectMapper, annotationsToUse));
        providers.add(new ExceptionResponseMapper());
        return providers;
    }


}
