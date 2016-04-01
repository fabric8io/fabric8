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
package io.fabric8.repo.git;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.ServiceNames;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory for the {@link GitRepoClient} from a {@link KubernetesClient}
 */
public class GitRepoKubernetes {
    private static final transient Logger LOG = LoggerFactory.getLogger(GitRepoKubernetes.class);

    public static final String JENKINS_GOGS_USER = "JENKINS_GOGS_USER";
    public static final String JENKINS_GOGS_PASSWORD = "JENKINS_GOGS_PASSWORD";

    public static GitRepoClient createGitRepoClient(KubernetesClient kubernetes) {
        return createGitRepoClient(kubernetes, null, null);
    }

    public static GitRepoClient createGitRepoClient(KubernetesClient kubernetes, String userName, String password) {
        if (Strings.isNullOrBlank(userName)) {
            userName = Systems.getEnvVarOrSystemProperty(JENKINS_GOGS_USER, "gogsadmin");
        }
        if (Strings.isNullOrBlank(password)) {
            password = Systems.getEnvVarOrSystemProperty(JENKINS_GOGS_PASSWORD, "RedHat$1");
        }

        String namespace = KubernetesHelper.defaultNamespace();
        String address;
        try {
            address = KubernetesHelper.getServiceURL(kubernetes, ServiceNames.GOGS, namespace, "http", true);
            if (Strings.isNullOrBlank(address)) {
                LOG.warn("No Gogs service could be found in kubernetes " + namespace + " on address: " + kubernetes.getMasterUrl());
                return null;
            }
        } catch (IllegalArgumentException e) {
            LOG.warn("No Gogs service could be found in kubernetes " + namespace + " on address: " + kubernetes.getMasterUrl());
            return null;
        }
        LOG.info("Logging into Gogs at " + address + " as user " + userName);
        return new GitRepoClient(address, userName, password);
    }
}
