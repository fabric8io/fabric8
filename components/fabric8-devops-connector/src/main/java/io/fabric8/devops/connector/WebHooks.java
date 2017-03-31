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
package io.fabric8.devops.connector;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.repo.git.CreateWebhookDTO;
import io.fabric8.repo.git.GitRepoClient;
import io.fabric8.repo.git.RepositoryDTO;
import io.fabric8.repo.git.WebHookDTO;
import io.fabric8.repo.git.WebhookConfig;
import io.fabric8.utils.Objects;
import org.slf4j.Logger;

import java.util.List;

import static io.fabric8.utils.jaxrs.JsonHelper.toJson;

/**
 * A helper class for registering web hooks
 */
public class WebHooks {

    /**
     * Creates a webook in the given gogs repo for the user and password if the webhook does not already exist
     */
    public static boolean createGogsWebhook(GitRepoClient repoClient, Logger log, String gogsUser, String repoName, String webhookUrl, String webhookSecret) throws JsonProcessingException {
        if (repoClient == null) {
            log.info("Cannot create Gogs webhooks as no Gogs service could be found or created");
            return false;
        }
        String gogsAddress = repoClient.getAddress();
        log.info("Querying webhooks in gogs at address: " + gogsAddress + " for user " + gogsUser + " repoName: " + repoName);

        RepositoryDTO repository = repoClient.getRepository(gogsUser, repoName);
        if (repository == null) {
            log.info("No repository found for user: " + gogsUser + " repo: " + repoName + " so cannot create any web hooks");
            //return false;
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
