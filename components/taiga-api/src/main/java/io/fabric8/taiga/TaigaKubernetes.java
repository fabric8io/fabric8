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
package io.fabric8.taiga;

import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.ServiceNames;
import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory for the {@link TaigaClient} from a {@link KubernetesClient}
 */
public class TaigaKubernetes {
    private static final transient Logger LOG = LoggerFactory.getLogger(TaigaKubernetes.class);

    public static TaigaClient createTaiga(KubernetesClient kubernetes) {
        String userName = Systems.getEnvVarOrSystemProperty("TAIGA_USERNAME", "admin");
        String password = Systems.getEnvVarOrSystemProperty("TAIGA_PASSWORD", "123123");

        String namespace = kubernetes.getNamespace();
        String address = null;
        try {
            address = kubernetes.getServiceURL(ServiceNames.TAIGA, namespace, "http", true);
            if (Strings.isNullOrBlank(address)) {
                LOG.warn("No Taiga service could be found in kubernetes " + namespace + " on address: " + kubernetes.getAddress());
                return null;
            }
        } catch (IllegalArgumentException e) {
            LOG.warn("No Taiga service could be found in kubernetes " + namespace + " on address: " + kubernetes.getAddress());
            return null;
        }
        LOG.info("Logging into Taiga at " + address + " as user " + userName);
        return new TaigaClient(address, userName, password);
    }
}
