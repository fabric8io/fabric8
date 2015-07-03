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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.repo.git.CreateWebhookDTO;
import io.fabric8.repo.git.GitRepoClient;
import io.fabric8.repo.git.GitRepoKubernetes;
import io.fabric8.repo.git.RepositoryDTO;
import io.fabric8.repo.git.WebHookDTO;
import io.fabric8.repo.git.WebhookConfig;
import io.fabric8.utils.Objects;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;

import static io.fabric8.utils.cxf.JsonHelper.toJson;

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
            Log log = getLog();
            String gogsUser = this.gogsUsername;
            String gogsPwd = this.gogsPassword;
            String repoName = this.repo;
            String webhookUrlValue = this.webhookUrl;
            String webhookSecret = this.secret;

            GitRepoClient gitRepoClient = GitRepoKubernetes.createGitRepoClient(getKubernetes(), gogsUser, gogsPassword);
            if (gitRepoClient == null) {
                getLog().error("No Gogs service found in kubernetes at address " + kubernetes.getAddress() + " namespace " + kubernetes.getNamespace());
            } else {
                createGogsWebhook(gitRepoClient, log, gogsUser, repoName, webhookUrlValue, webhookSecret);
            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to load environment schemas: " + e, e);
        }
    }

    /**
     * Creates a webook in the given gogs repo for the user and password if the webhook does not already exist
     */
    public static boolean createGogsWebhook(GitRepoClient repoClient, Log log, String gogsUser, String repoName, String webhookUrl, String webhookSecret) throws MojoExecutionException, JsonProcessingException {
        if (repoClient == null) {
            log.info("Cannot create Gogs webhooks as no Gogs service could be found or created");
            return false;
        }
        String gogsAddress = repoClient.getAddress();
        log.info("Querying webhooks in gogs at address: " + gogsAddress + " for user " + gogsUser + " repoName: " + repoName);

        RepositoryDTO repository = repoClient.getRepository(gogsUser, repoName);
        if (repository == null) {
            log.info("No repository found for user: " + gogsUser + " repo: " + repoName + " so cannot create any web hooks");
            return false;
        }
        List<WebHookDTO> webhooks = repoClient.getWebhooks(gogsUser, repoName);
        for (WebHookDTO webhook : webhooks) {
            String url = null;
            WebhookConfig config = webhook.getConfig();
            if (config != null) {
                url = config.getUrl();
                if (Objects.equal(webhookUrl, url)) {
                    log.info("Already has webhook for: " + url + " so not creating again");
                    return false;
                }
                log.info("Ignoring webhook " + url + " from: " + toJson(config));
            }
        }
        CreateWebhookDTO createWebhook = new CreateWebhookDTO();
        createWebhook.setType("gogs");
        WebhookConfig config = createWebhook.getConfig();
        config.setUrl(webhookUrl);
        config.setSecret(webhookSecret);
        WebHookDTO webhook = repoClient.createWebhook(gogsUser, repoName, createWebhook);
        if (log.isDebugEnabled()) {
            log.debug("Got created web hook: " + toJson(webhook));
        }
        log.info("Created webhook for " + webhookUrl + " for user: " + gogsUser + " repoName: " + repoName + " on gogs URL: " + gogsAddress);
        return true;
    }
}
