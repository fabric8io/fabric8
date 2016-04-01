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
package io.fabric8.taiga;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.ServiceNames;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory for the {@link TaigaClient} from a {@link KubernetesClient}
 */
public class TaigaKubernetes {
    private static final transient Logger LOG = LoggerFactory.getLogger(TaigaKubernetes.class);

    public static TaigaClient createTaiga(KubernetesClient kubernetes, String namespace) {
        String userName = Systems.getEnvVarOrSystemProperty("TAIGA_USERNAME", "admin");
        String password = Systems.getEnvVarOrSystemProperty("TAIGA_PASSWORD", "123123");

        String address = null;
        try {
            address = KubernetesHelper.getServiceURL(kubernetes, ServiceNames.TAIGA, namespace, "http", true);
            if (Strings.isNullOrBlank(address)) {
                LOG.warn("No Taiga service could be found in kubernetes " + namespace + " on address: " + kubernetes.getMasterUrl());
                return null;
            }
        } catch (Exception e) {
            LOG.warn("No Taiga service could be found in kubernetes " + namespace + " on address: " + kubernetes.getMasterUrl());
            return null;
        }
        LOG.info("Logging into Taiga at " + address + " as user " + userName);
        return new TaigaClient(address, userName, password);
    }
}
