/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.maven;

import io.fabric8.devops.connector.WebHooks;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.repo.git.GitRepoClient;
import io.fabric8.repo.git.GitRepoKubernetes;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a web hook in a gogs repository
 */
@Mojo(name = "create-gogs-webhook", requiresProject = false)
public class CreateGogsWebhook extends AbstractNamespacedMojo {

    /**
     * The URL of the webhook to register
     */
    @Parameter(property = "webhookUrl", required = true)
    private String webhookUrl;

    /**
     * The gogs repo to add the webhook to
     */
    @Parameter(property = "repo", required = true)
    private String repo;

    /**
     * The user name to use in gogs
     */
    @Parameter(property = "gogsUsername", defaultValue = "${JENKINS_GOGS_USER}")
    private String gogsUsername;

    /**
     * The password to use in gogs
     */
    @Parameter(property = "gogsPassword", defaultValue = "${JENKINS_GOGS_PASSWORD}")
    private String gogsPassword;

    /**
     * The secret added to the webhook
     */
    @Parameter(property = "secret", defaultValue = "secret101")
    private String secret;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            KubernetesClient kubernetes = getKubernetes();
            final Log log = getLog();
            String gogsUser = this.gogsUsername;
            String gogsPwd = this.gogsPassword;
            String repoName = this.repo;
            String webhookUrlValue = this.webhookUrl;
            String webhookSecret = this.secret;

            GitRepoClient gitRepoClient = GitRepoKubernetes.createGitRepoClient(getKubernetes(), gogsUser, gogsPassword);
            if (gitRepoClient == null) {
                getLog().error("No Gogs service found in kubernetes at address " + kubernetes.getMasterUrl() + " namespace " + getNamespace());
            } else {
                // TODO should ideally reuse the mojo Log
                Logger logger = LoggerFactory.getLogger(CreateGogsWebhook.class);
                WebHooks.createGogsWebhook(gitRepoClient, logger, gogsUser, repoName, webhookUrlValue, webhookSecret);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to load environment schemas: " + e, e);
        }
    }

}
