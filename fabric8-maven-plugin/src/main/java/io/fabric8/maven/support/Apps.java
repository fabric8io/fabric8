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
package io.fabric8.maven.support;

import io.fabric8.utils.Closeables;
import io.fabric8.utils.Strings;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 */
public class Apps {

    /**
     * Posts a file to the git repository
     */
    public static HttpResponse postFileToGit(File file, String user, String password, String consoleUrl, String branch, String path, Logger logger) throws URISyntaxException, IOException {
        HttpClientBuilder builder = HttpClients.custom();
        if (Strings.isNotBlank(user) && Strings.isNotBlank(password)) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    new AuthScope("localhost", 443),
                    new UsernamePasswordCredentials(user, password));
            builder = builder
                    .setDefaultCredentialsProvider(credsProvider);
        }

        CloseableHttpClient client = builder.build();
        try {

            String url = consoleUrl;
            if (!url.endsWith("/")) {
                url += "/";
            }
            url += "git/";
            url += branch;
            if (!path.startsWith("/")) {
                url += "/";
            }
            url += path;

            logger.info("Posting App Zip " + file.getName() + " to " + url);
            URI buildUrl = new URI(url);
            HttpPost post = new HttpPost(buildUrl);

            // use multi part entity format
            FileBody zip = new FileBody(file);
            HttpEntity entity = MultipartEntityBuilder.create()
                    .addPart(file.getName(), zip)
                    .build();
            post.setEntity(entity);
            // post.setEntity(new FileEntity(file));

            HttpResponse response = client.execute(URIUtils.extractHost(buildUrl), post);
            logger.info("Response: " + response);
            if (response != null) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode < 200 || statusCode >= 300) {
                    throw new IllegalStateException("Failed to post App Zip to: " + url + " " + response);
                }
            }
            return response;
        } finally {
            Closeables.closeQuietly(client);
        }
    }
}
