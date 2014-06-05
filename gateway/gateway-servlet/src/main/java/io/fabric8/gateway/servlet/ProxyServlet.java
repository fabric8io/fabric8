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
package io.fabric8.gateway.servlet;

import io.fabric8.common.util.IOHelpers;
import io.fabric8.gateway.model.HttpProxyRule;
import io.fabric8.gateway.model.HttpProxyRuleBase;
import io.fabric8.gateway.servlet.support.NonBindingSocketFactory;
import io.fabric8.gateway.servlet.support.ProxySupport;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.protocol.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * Based on code from http://edwardstx.net/2010/06/http-proxy-servlet/
 */
public abstract class ProxyServlet extends HttpServlet {
    private static final transient Logger LOG = LoggerFactory.getLogger(ProxyServlet.class);

    /**
     * Serialization UID.
     */
    private static final long serialVersionUID = 1L;
    /**
     * Key for redirect location header.
     */
    private static final String STRING_LOCATION_HEADER = "Location";
    /**
     * Key for content type header.
     */
    private static final String STRING_CONTENT_TYPE_HEADER_NAME = "Content-Type";

    /**
     * Key for content length header.
     */
    private static final String STRING_CONTENT_LENGTH_HEADER_NAME = "Content-Length";

    private static final String[] IGNORE_HEADER_NAMES = {STRING_CONTENT_LENGTH_HEADER_NAME, "Origin", "Authorization"};


    /**
     * Key for host header
     */
    private static final String STRING_HOST_HEADER_NAME = "Host";
    /**
     * The directory to use to temporarily store uploaded files
     */
    private static final File FILE_UPLOAD_TEMP_DIRECTORY = new File(System.getProperty("java.io.tmpdir"));

    private HttpMappingRuleResolver resolver = new HttpMappingRuleResolver();

    /**
     * The maximum size for uploaded files in bytes. Default value is 5MB.
     */
    private int intMaxFileUploadSize = 5 * 1024 * 1024;

    /**
     * Initialize the <code>ProxyServlet</code>
     *
     * @param config The Servlet configuration passed in by the servlet container
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        HttpProxyRuleBase ruleBase = new HttpProxyRuleBase();
        loadRuleBase(config, ruleBase);
        resolver.setMappingRules(ruleBase);
        Protocol.registerProtocol("http", new Protocol("http", new NonBindingSocketFactory(), 80));
        Protocol.registerProtocol("https", new Protocol("https", new NonBindingSocketFactory(), 443));
    }

    /**
     * load the mapping rules from the servlet context; could use a Java DSL, the XML DSL or load from a database
     */
    protected abstract void loadRuleBase(ServletConfig config, HttpProxyRuleBase ruleBase) throws ServletException;

    /**
     * Performs an HTTP GET request
     *
     * @param httpServletRequest  The {@link javax.servlet.http.HttpServletRequest} object passed
     *                            in by the servlet engine representing the
     *                            client request to be proxied
     * @param httpServletResponse The {@link javax.servlet.http.HttpServletResponse} object by which
     *                            we can send a proxied response to the client
     */
    @Override
    public void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException, ServletException {
        // Create a GET request
        ProxyDetails proxyDetails = createProxyDetails(httpServletRequest, httpServletResponse);
        if (!proxyDetails.isValid()) {
            noMappingFound(httpServletRequest, httpServletResponse);
        } else {
            GetMethod getMethodProxyRequest = new GetMethod(proxyDetails.getStringProxyURL());
            // Forward the request headers
            setProxyRequestHeaders(proxyDetails, httpServletRequest, getMethodProxyRequest);
            // Execute the proxy request
            this.executeProxyRequest(proxyDetails, getMethodProxyRequest, httpServletRequest, httpServletResponse);
        }
    }

    /**
     * Performs an HTTP POST request
     *
     * @param httpServletRequest  The {@link javax.servlet.http.HttpServletRequest} object passed
     *                            in by the servlet engine representing the
     *                            client request to be proxied
     * @param httpServletResponse The {@link javax.servlet.http.HttpServletResponse} object by which
     *                            we can send a proxied response to the client
     */
    @Override
    public void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException, ServletException {
        // Create a standard POST request
        ProxyDetails proxyDetails = createProxyDetails(httpServletRequest, httpServletResponse);
        if (!proxyDetails.isValid()) {
            noMappingFound(httpServletRequest, httpServletResponse);
        } else {
            PostMethod postMethodProxyRequest = new PostMethod(proxyDetails.getStringProxyURL());
            // Forward the request headers
            setProxyRequestHeaders(proxyDetails, httpServletRequest, postMethodProxyRequest);
            // Check if this is a mulitpart (file upload) POST
            if (ServletFileUpload.isMultipartContent(httpServletRequest)) {
                this.handleMultipartPost(postMethodProxyRequest, httpServletRequest);
            } else {
                this.handleEntity(postMethodProxyRequest, httpServletRequest);
            }
            // Execute the proxy request
            this.executeProxyRequest(proxyDetails, postMethodProxyRequest, httpServletRequest, httpServletResponse);
        }
    }

    /**
     * Performs an HTTP PUT request
     *
     * @param httpServletRequest  The {@link javax.servlet.http.HttpServletRequest} object passed
     *                            in by the servlet engine representing the
     *                            client request to be proxied
     * @param httpServletResponse The {@link javax.servlet.http.HttpServletResponse} object by which
     *                            we can send a proxied response to the client
     */
    @Override
    public void doPut(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException, ServletException {
        ProxyDetails proxyDetails = createProxyDetails(httpServletRequest, httpServletResponse);
        if (!proxyDetails.isValid()) {
            noMappingFound(httpServletRequest, httpServletResponse);
        } else {
            PutMethod putMethodProxyRequest = new PutMethod(proxyDetails.getStringProxyURL());
            setProxyRequestHeaders(proxyDetails, httpServletRequest, putMethodProxyRequest);
            if (ServletFileUpload.isMultipartContent(httpServletRequest)) {
                handleMultipartPost(putMethodProxyRequest, httpServletRequest);
            } else {
                handleEntity(putMethodProxyRequest, httpServletRequest);
            }
            executeProxyRequest(proxyDetails, putMethodProxyRequest, httpServletRequest, httpServletResponse);
        }
    }

    /**
     * Performs an HTTP DELETE request
     *
     * @param httpServletRequest  The {@link javax.servlet.http.HttpServletRequest} object passed
     *                            in by the servlet engine representing the
     *                            client request to be proxied
     * @param httpServletResponse The {@link javax.servlet.http.HttpServletResponse} object by which
     *                            we can send a proxied response to the client
     */
    @Override
    public void doDelete(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException, ServletException {
        ProxyDetails proxyDetails = createProxyDetails(httpServletRequest, httpServletResponse);
        if (!proxyDetails.isValid()) {
            noMappingFound(httpServletRequest, httpServletResponse);
        } else {
            DeleteMethod deleteMethodProxyRequest = new DeleteMethod(proxyDetails.getStringProxyURL());
            // Forward the request headers
            setProxyRequestHeaders(proxyDetails, httpServletRequest, deleteMethodProxyRequest);
            // Execute the proxy request
            executeProxyRequest(proxyDetails, deleteMethodProxyRequest, httpServletRequest, httpServletResponse);
        }
    }

    @Override
    protected void doOptions(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        ProxyDetails proxyDetails = createProxyDetails(httpServletRequest, httpServletResponse);
        if (!proxyDetails.isValid()) {
            noMappingFound(httpServletRequest, httpServletResponse);
        } else {
            OptionsMethod optionsMethodProxyRequest = new OptionsMethod(proxyDetails.getStringProxyURL());
            setProxyRequestHeaders(proxyDetails, httpServletRequest, optionsMethodProxyRequest);
            executeProxyRequest(proxyDetails, optionsMethodProxyRequest, httpServletRequest, httpServletResponse);
        }
    }

    protected ProxyDetails createProxyDetails(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        HttpMappingResult mappingRule = getResolver().findMappingRule(httpServletRequest, httpServletResponse);
        final HttpProxyRule proxyRule = mappingRule.getProxyRule();
        if (mappingRule != null) {
            String destinationUrl = mappingRule.getDestinationUrl(new HttpClientRequestFacade(httpServletRequest, httpServletResponse));
            if (destinationUrl != null) {
                return new ProxyDetails(true, destinationUrl, proxyRule);
            }
        }
        return new ProxyDetails(false, null, proxyRule);
    }

    protected void noMappingFound(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "No endpoint could be found for " + httpServletRequest.getRequestURI());
    }


    /**
     * Sets up the given {@link EntityEnclosingMethod} to send the same multipart
     * data as was sent in the given {@link javax.servlet.http.HttpServletRequest}
     *
     * @param entityEnclosingMethod The {@link EntityEnclosingMethod} that we are
     *                               configuring to send a multipart request
     * @param httpServletRequest     The {@link javax.servlet.http.HttpServletRequest} that contains
     *                               the mutlipart data to be sent via the {@link EntityEnclosingMethod}
     */
    private void handleMultipartPost(EntityEnclosingMethod entityEnclosingMethod, HttpServletRequest httpServletRequest)
            throws ServletException {
        // Create a factory for disk-based file items
        DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();
        // Set factory constraints
        diskFileItemFactory.setSizeThreshold(this.getMaxFileUploadSize());
        diskFileItemFactory.setRepository(FILE_UPLOAD_TEMP_DIRECTORY);
        // Create a new file upload handler
        ServletFileUpload servletFileUpload = new ServletFileUpload(diskFileItemFactory);
        // Parse the request
        try {
            // Get the multipart items as a list
            List<FileItem> listFileItems = (List<FileItem>) servletFileUpload.parseRequest(httpServletRequest);
            // Create a list to hold all of the parts
            List<Part> listParts = new ArrayList<Part>();
            // Iterate the multipart items list
            for (FileItem fileItemCurrent : listFileItems) {
                // If the current item is a form field, then create a string part
                if (fileItemCurrent.isFormField()) {
                    StringPart stringPart = new StringPart(
                            fileItemCurrent.getFieldName(), // The field name
                            fileItemCurrent.getString()     // The field value
                    );
                    // Add the part to the list
                    listParts.add(stringPart);
                } else {
                    // The item is a file upload, so we create a FilePart
                    FilePart filePart = new FilePart(
                            fileItemCurrent.getFieldName(),    // The field name
                            new ByteArrayPartSource(
                                    fileItemCurrent.getName(), // The uploaded file name
                                    fileItemCurrent.get()      // The uploaded file contents
                            )
                    );
                    // Add the part to the list
                    listParts.add(filePart);
                }
            }
            MultipartRequestEntity multipartRequestEntity = new MultipartRequestEntity(
                    listParts.toArray(new Part[]{}),
                    entityEnclosingMethod.getParams()
            );
            entityEnclosingMethod.setRequestEntity(multipartRequestEntity);
            // The current content-type header (received from the client) IS of
            // type "multipart/form-data", but the content-type header also
            // contains the chunk boundary string of the chunks. Currently, this
            // header is using the boundary of the client request, since we
            // blindly copied all headers from the client request to the proxy
            // request. However, we are creating a new request with a new chunk
            // boundary string, so it is necessary that we re-set the
            // content-type string to reflect the new chunk boundary string
            entityEnclosingMethod.setRequestHeader(STRING_CONTENT_TYPE_HEADER_NAME, multipartRequestEntity.getContentType());
        } catch (FileUploadException fileUploadException) {
            throw new ServletException(fileUploadException);
        }
    }

    /**
     * Sets up the given {@link PostMethod} to send the same standard
     * data as was sent in the given {@link javax.servlet.http.HttpServletRequest}
     *
     * @param entityEnclosingMethod The {@link EntityEnclosingMethod} that we are
     *                               configuring to send a standard request
     * @param httpServletRequest     The {@link javax.servlet.http.HttpServletRequest} that contains
     *                               the data to be sent via the {@link EntityEnclosingMethod}
     */
    @SuppressWarnings("unchecked")
    private void handleEntity(EntityEnclosingMethod entityEnclosingMethod, HttpServletRequest httpServletRequest) throws IOException {
        // Get the client POST data as a Map
        Map<String, String[]> mapPostParameters = (Map<String, String[]>) httpServletRequest.getParameterMap();
        // Create a List to hold the NameValuePairs to be passed to the PostMethod
        List<NameValuePair> listNameValuePairs = new ArrayList<NameValuePair>();
        // Iterate the parameter names
        for (String stringParameterName : mapPostParameters.keySet()) {
            // Iterate the values for each parameter name
            String[] stringArrayParameterValues = mapPostParameters.get(stringParameterName);
            for (String stringParamterValue : stringArrayParameterValues) {
                // Create a NameValuePair and store in list
                NameValuePair nameValuePair = new NameValuePair(stringParameterName, stringParamterValue);
                listNameValuePairs.add(nameValuePair);
            }
        }
        RequestEntity entity = null;
        String contentType = httpServletRequest.getContentType();
        if (contentType != null) {
            contentType = contentType.toLowerCase();
            if (contentType.contains("json") || contentType.contains("xml") || contentType.contains("application") || contentType.contains("text")) {
                String body = IOHelpers.readFully(httpServletRequest.getReader());
                entity = new StringRequestEntity(body, contentType, httpServletRequest.getCharacterEncoding());
                entityEnclosingMethod.setRequestEntity(entity);
            }
        }
        NameValuePair[] parameters = listNameValuePairs.toArray(new NameValuePair[]{});
        if (entity != null) {
            // TODO add as URL parameters?
            //postMethodProxyRequest.addParameters(parameters);
        } else {
            // Set the proxy request POST data
            if (entityEnclosingMethod instanceof PostMethod) {
                ((PostMethod)entityEnclosingMethod).setRequestBody(parameters);
            }
        }
    }

    /**
     * Executes the {@link HttpMethod} passed in and sends the proxy response
     * back to the client via the given {@link javax.servlet.http.HttpServletResponse}
     *
     * @param proxyDetails
     * @param httpMethodProxyRequest An object representing the proxy request to be made
     * @param httpServletResponse    An object by which we can send the proxied
     *                               response back to the client
     * @throws java.io.IOException            Can be thrown by the {@link HttpClient}.executeMethod
     * @throws javax.servlet.ServletException Can be thrown to indicate that another error has occurred
     */
    private void executeProxyRequest(
            ProxyDetails proxyDetails, HttpMethod httpMethodProxyRequest,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse)
            throws IOException, ServletException {
        httpMethodProxyRequest.setDoAuthentication(false);
        httpMethodProxyRequest.setFollowRedirects(false);

        // Create a default HttpClient
        HttpClient httpClient = proxyDetails.createHttpClient(httpMethodProxyRequest);

        // Execute the request
        int intProxyResponseCode = httpClient.executeMethod(httpMethodProxyRequest);

        // Check if the proxy response is a redirect
        // The following code is adapted from org.tigris.noodle.filters.CheckForRedirect
        // Hooray for open source software
        if (intProxyResponseCode >= HttpServletResponse.SC_MULTIPLE_CHOICES /* 300 */
                && intProxyResponseCode < HttpServletResponse.SC_NOT_MODIFIED /* 304 */) {
            String stringStatusCode = Integer.toString(intProxyResponseCode);
            String stringLocation = httpMethodProxyRequest.getResponseHeader(STRING_LOCATION_HEADER).getValue();
            if (stringLocation == null) {
                throw new ServletException("Received status code: " + stringStatusCode
                        + " but no " + STRING_LOCATION_HEADER + " header was found in the response");
            }
            // Modify the redirect to go to this proxy servlet rather that the proxied host
            String stringMyHostName = httpServletRequest.getServerName();
            if (httpServletRequest.getServerPort() != 80) {
                stringMyHostName += ":" + httpServletRequest.getServerPort();
            }
            stringMyHostName += httpServletRequest.getContextPath();
            httpServletResponse.sendRedirect(stringLocation.replace(proxyDetails.getProxyHostAndPort() + proxyDetails.getProxyPath(), stringMyHostName));
            return;
        } else if (intProxyResponseCode == HttpServletResponse.SC_NOT_MODIFIED) {
            // 304 needs special handling.  See:
            // http://www.ics.uci.edu/pub/ietf/http/rfc1945.html#Code304
            // We get a 304 whenever passed an 'If-Modified-Since'
            // header and the data on disk has not changed; server
            // responds w/ a 304 saying I'm not going to send the
            // body because the file has not changed.
            httpServletResponse.setIntHeader(STRING_CONTENT_LENGTH_HEADER_NAME, 0);
            httpServletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        // Pass the response code back to the client
        httpServletResponse.setStatus(intProxyResponseCode);

        // Pass response headers back to the client
        Header[] headerArrayResponse = httpMethodProxyRequest.getResponseHeaders();
        for (Header header : headerArrayResponse) {
            if (!ProxySupport.isHopByHopHeader(header.getName())) {
                if (ProxySupport.isSetCookieHeader(header)) {
                    HttpProxyRule proxyRule = proxyDetails.getProxyRule();
                    String setCookie = ProxySupport.replaceCookieAttributes(header.getValue(),
                            proxyRule.getCookiePath(),
                            proxyRule.getCookieDomain());
                    httpServletResponse.setHeader(header.getName(), setCookie);
                } else {
                    httpServletResponse.setHeader(header.getName(), header.getValue());
                }
            }
        }

        // check if we got data, that is either the Content-Length > 0
        // or the response code != 204
        int code = httpMethodProxyRequest.getStatusCode();
        boolean noData = code == HttpStatus.SC_NO_CONTENT;
        if (!noData) {
            String length = httpServletRequest.getHeader(STRING_CONTENT_LENGTH_HEADER_NAME);
            if (length != null && "0".equals(length.trim())) {
                noData = true;
            }
        }
        LOG.trace("Response has data? {}", !noData);

        if (!noData) {
            // Send the content to the client
            InputStream inputStreamProxyResponse = httpMethodProxyRequest.getResponseBodyAsStream();
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStreamProxyResponse);
            OutputStream outputStreamClientResponse = httpServletResponse.getOutputStream();
            int intNextByte;
            while ((intNextByte = bufferedInputStream.read()) != -1) {
                outputStreamClientResponse.write(intNextByte);
            }
        }
    }

    public String getServletInfo() {
        return "Fabric8 Gateway Proxy Servlet";
    }


    public HttpMappingRuleResolver getResolver() {
        return resolver;
    }

    /**
     * Retrieves all of the headers from the servlet request and sets them on
     * the proxy request
     *
     * @param proxyDetails
     * @param httpServletRequest     The request object representing the client's
     *                               request to the servlet engine
     * @param httpMethodProxyRequest The request that we are about to send to
     */
    private void setProxyRequestHeaders(ProxyDetails proxyDetails, HttpServletRequest httpServletRequest, HttpMethod httpMethodProxyRequest) {
        // Get an Enumeration of all of the header names sent by the client
        Enumeration<?> enumerationOfHeaderNames = httpServletRequest.getHeaderNames();
        while (enumerationOfHeaderNames.hasMoreElements()) {
            String stringHeaderName = (String) enumerationOfHeaderNames.nextElement();

            if (stringHeaderName.equalsIgnoreCase(STRING_CONTENT_LENGTH_HEADER_NAME) ||
                    ProxySupport.isHopByHopHeader(stringHeaderName))
                continue;
            // As per the Java Servlet API 2.5 documentation:
            //		Some headers, such as Accept-Language can be sent by clients
            //		as several headers each with a different value rather than
            //		sending the header as a comma separated list.
            // Thus, we get an Enumeration of the header values sent by the client
            Enumeration<?> enumerationOfHeaderValues = httpServletRequest.getHeaders(stringHeaderName);
            while (enumerationOfHeaderValues.hasMoreElements()) {
                String stringHeaderValue = (String) enumerationOfHeaderValues.nextElement();
                // In case the proxy host is running multiple virtual servers,
                // rewrite the Host header to ensure that we get content from
                // the correct virtual server
                if (stringHeaderName.equalsIgnoreCase(STRING_HOST_HEADER_NAME)) {
                    stringHeaderValue = proxyDetails.getProxyHostAndPort();
                }
                Header header = new Header(stringHeaderName, stringHeaderValue);
                // Set the same header on the proxy request
                httpMethodProxyRequest.setRequestHeader(header);
            }
        }
    }


    private int getMaxFileUploadSize() {
        return this.intMaxFileUploadSize;
    }

    private void setMaxFileUploadSize(int intMaxFileUploadSizeNew) {
        this.intMaxFileUploadSize = intMaxFileUploadSizeNew;
    }
}