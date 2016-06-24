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
package io.fabric8.letschat;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.ServiceNames;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory for the {@link LetsChatClient} from a {@link KubernetesClient}
 */
public class LetsChatKubernetes {
    private static final transient Logger LOG = LoggerFactory.getLogger(LetsChatKubernetes.class);

    public static final String LETSCHAT_HUBOT_USERNAME = "LETSCHAT_HUBOT_USERNAME";
    public static final String LETSCHAT_HUBOT_PASSWORD = "LETSCHAT_HUBOT_PASSWORD";
    public static final String LETSCHAT_HUBOT_TOKEN = "LETSCHAT_HUBOT_TOKEN";

    public static LetsChatClient createLetsChat(KubernetesClient kubernetes) {
        String userName = Systems.getEnvVarOrSystemProperty(LETSCHAT_HUBOT_USERNAME, "fabric8");
        String password = Systems.getEnvVarOrSystemProperty(LETSCHAT_HUBOT_PASSWORD, "RedHat$1");
        String token = Systems.getEnvVarOrSystemProperty(LETSCHAT_HUBOT_TOKEN);

        String namespace = KubernetesHelper.defaultNamespace();
        String address;
        try {
            address = KubernetesHelper.getServiceURL(kubernetes, ServiceNames.LETSCHAT, namespace, "http", true);
            if (Strings.isNullOrBlank(address)) {
                LOG.warn("No LetsChat service could be found in kubernetes " + namespace + " on address: " + kubernetes.getMasterUrl());
                return null;
            }
        } catch (Exception e) {
            LOG.warn("No LetsChat service could be found in kubernetes " + namespace + " on address: " + kubernetes.getMasterUrl());
            return null;
        }
        LOG.info("Logging into LetsChat at " + address + " as user " + userName);
        return new LetsChatClient(address, userName, password, token);
    }
}
