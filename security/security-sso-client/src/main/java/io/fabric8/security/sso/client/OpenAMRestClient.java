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

package io.fabric8.security.sso.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Simple helper class that makes it easy to invoke on OpenAM's REST interface
 */
public class OpenAMRestClient {

    private static Logger LOG = LoggerFactory.getLogger(OpenAMRestClient.class);

    protected String OpenAMRealm = "/";
    protected String OpenAMService = "activemq";
    protected String OpenAMHostName = "localhost";
    protected String OpenAMPort = "8080";
    protected String OpenAMMethod = "http";
    protected String OpenAMURLPrefix = "opensso";
    protected String ServicePrefix = "http://localhost:61616/activemq";
    protected boolean debug = false;

    /**
     * Authenticates the supplied username and password and returns a
     * token from the security token service
     * @param username
     * @param password
     * @return
     */
    public String authenticate(String username, String password) {
        String rc = doInvoke(getAuthenticateUrl(username, password));
        if (!rc.startsWith("token.id=")) {
            return null;
        }
        return rc.split("=")[1].trim();
    }

    /**
     * Checks to see if the supplied token is authorized to access
     * the supplied service using the supplied action (GET or POST)
     * @param service The service the token will be authorized against, will be
     * concatenated onto the ServicePrefix property
     * @param token The security token
     * @param action either GET or POST
     * @return
     */
    public boolean authorize(String service, String token, String action) {
        if (!service.startsWith("/")) {
            service = getServicePrefix() + "/" + service;
        } else {
            service = getServicePrefix() + service;
        }
        return checkResult(doInvoke(getAuthorizeUrl(service, token, action)));
    }

    /**
     * Checks to see if the supplied token is authorized to access
     * the supplied service using GET
     * @param service The service the token will be authorized against, will be concatenated onto the ServicePrefix property
     * @param token the security token obtained from a previous call to @authenticate
     * @return
     */
    public boolean authorize(String service, String token) {
        return authorize(service, token, "GET");
    }

    /**
     * Checks with the security token service to see if the supplied token is
     * a valid token
     * @param token
     * @return
     */
    public boolean isValidToken(String token) {
        return checkResult(doInvoke(getIsValidTokenUrl(token)));
    }

    /**
     * Returns the base URL of the OpenAM server this client instance will
     * invoke on
     * @return
     */
    public String getURLPrefix() {
        return OpenAMMethod + "://" + OpenAMHostName + ":" + OpenAMPort + "/" + OpenAMURLPrefix;
    }


    public void logout(String token) {
        doInvoke(getLogoutUrl(token));
    }

    @Override
    public String toString() {
        return "OpenAMRestClient{" +
                "OpenAMRealm='" + OpenAMRealm + '\'' +
                ", OpenAMService='" + OpenAMService + '\'' +
                ", OpenAMHostName='" + OpenAMHostName + '\'' +
                ", OpenAMPort='" + OpenAMPort + '\'' +
                ", OpenAMMethod='" + OpenAMMethod + '\'' +
                ", OpenAMURLPrefix='" + OpenAMURLPrefix + '\'' +
                ", ServicePrefix='" + ServicePrefix + '\'' +
                ", debug=" + debug +
                '}';
    }

    protected boolean checkResult(String result) {
        result = result.trim();
        if (!result.startsWith("boolean=")) {
            return false;
        }
        try {
            return Boolean.parseBoolean(result.split("=")[1]);
        } catch (Exception e) {
            return false;
        }
    }

    protected String read(InputStream is) throws IOException {
        byte b[] = new byte[is.available()];
        is.read(b);
        return new String(b);
    }

    protected String doInvoke(String url) {
        try {
            if (debug) {
                LOG.info("Sending : {}", url);
            }
            HttpURLConnection auth = (HttpURLConnection)new URL(url).openConnection();
            auth.setRequestMethod("POST");
            auth.connect();
            String rc = "";
            if (auth.getResponseCode() == HttpURLConnection.HTTP_OK) {
                rc = read(auth.getInputStream());
            } else {
                rc = read(auth.getErrorStream());
            }
            auth.disconnect();
            if (debug) {
                LOG.info("Got : {}", rc);
            }
            return rc;
        } catch (Exception e) {
            LOG.warn("Exception invoking on REST API", e);
            throw new RuntimeException("Exception invoking on REST API : " + e.getMessage());
        }
    }

    protected String getLogoutUrl(String token) {
        return String.format("%s/identity/logout?subjectid=%s", getURLPrefix(), token);
    }

    protected String getIsValidTokenUrl(String token) {
        return String.format("%s/identity/isTokenValid?tokenid=%s", getURLPrefix(), token);
    }

    protected String getAuthorizeUrl(String uri, String token, String action) {
        return String.format("%s/identity/authorize?uri=%s&action=%s&subjectid=%s", getURLPrefix(), uri, action, token);
    }

    protected String getAuthenticateUrl(String username, String password) {
        return String.format("%s/identity/authenticate?username=%s&password=%s", getURLPrefix(), username, password);
    }

    public String getOpenAMRealm() {
        return OpenAMRealm;
    }

    public void setOpenAMRealm(String openAMRealm) {
        OpenAMRealm = openAMRealm;
    }

    public String getOpenAMService() {
        return OpenAMService;
    }

    public void setOpenAMService(String openAMService) {
        OpenAMService = openAMService;
    }

    public String getOpenAMHostName() {
        return OpenAMHostName;
    }

    public void setOpenAMHostName(String openAMHostName) {
        OpenAMHostName = openAMHostName;
    }

    public String getOpenAMPort() {
        return OpenAMPort;
    }

    public void setOpenAMPort(String openAMPort) {
        OpenAMPort = openAMPort;
    }

    public String getOpenAMMethod() {
        return OpenAMMethod;
    }

    public void setOpenAMMethod(String openAMMethod) {
        OpenAMMethod = openAMMethod;
    }

    public String getOpenAMURLPrefix() {
        return OpenAMURLPrefix;
    }

    public void setOpenAMURLPrefix(String openAMURLPrefix) {
        OpenAMURLPrefix = openAMURLPrefix;
    }

    public String getServicePrefix() {
        return ServicePrefix;
    }

    public void setServicePrefix(String servicePrefix) {
        ServicePrefix = servicePrefix;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
