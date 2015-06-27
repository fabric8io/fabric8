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
package io.fabric8.letschat;

import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.ServiceNames;
import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory for the {@link LetsChatClient} from a {@link KubernetesClient}
 */
public class LetsChatKubernetes {
    private static final transient Logger LOG = LoggerFactory.getLogger(LetsChatKubernetes.class);

    public static LetsChatClient createLetsChat(KubernetesClient kubernetes) {
        String userName = Systems.getEnvVarOrSystemProperty("LETSCHAT_USERNAME", "admin");
        String password = Systems.getEnvVarOrSystemProperty("LETSCHAT_PASSWORD", "123123");
        String token = Systems.getEnvVarOrSystemProperty("LETSCHAT_TOKEN");

        String namespace = kubernetes.getNamespace();
        String address;
        try {
            address = kubernetes.getServiceURL(ServiceNames.LETSCHAT, namespace, "http", true);
            if (Strings.isNullOrBlank(address)) {
                LOG.warn("No LetsChat service could be found in kubernetes " + namespace + " on address: " + kubernetes.getAddress());
                return null;
            }
        } catch (IllegalArgumentException e) {
            LOG.warn("No Taiga service could be found in kubernetes " + namespace + " on address: " + kubernetes.getAddress());
            return null;
        }
        LOG.info("Logging into LetsChat at " + address + " as user " + userName);
        return new LetsChatClient(address, userName, password, token);
    }
}
