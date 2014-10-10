/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.jolokia;

import io.fabric8.common.util.Strings;
import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.KubernetesFactory;
import io.fabric8.kubernetes.api.model.ManifestContainer;
import io.fabric8.kubernetes.api.model.Port;
import org.jolokia.client.J4pClient;
import org.jolokia.client.J4pClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.fabric8.kubernetes.api.KubernetesHelper.getDockerIp;

/**
 * Provides simple access to jolokia clients for a cluster
 */
public class JolokiaClients {
    private static final transient Logger LOG = LoggerFactory.getLogger(JolokiaClients.class);

    private final Kubernetes kubernetes;
    private String user;
    private String password;

    public JolokiaClients() {
        this(new KubernetesFactory().createKubernetes());
    }

    public JolokiaClients(Kubernetes kubernetes) {
        this.kubernetes = kubernetes;
    }

    public Kubernetes getKubernetes() {
        return kubernetes;
    }

    /**
     * Returns the jolokia client for the given container
     */
    public J4pClient jolokiaClient(String host, ManifestContainer container) {
        if (container != null) {
            List<Port> ports = container.getPorts();
            for (Port port : ports) {
                Integer containerPort = port.getContainerPort();
                if (containerPort != null) {
                    if (containerPort == 8778) {
                        Integer hostPort = port.getHostPort();
                        if (hostPort != null) {
                            // if Kubernetes is running locally on a platform which doesn't support docker natively
                            // then docker containers will be on a different IP so lets check for localhost and
                            // switch to the docker IP if its available
                            if (host.equals("localhost") || host.equals("127.0.0.1")) {
                                String dockerIp = getDockerIp();
                                if (Strings.isNotBlank(dockerIp)) {
                                    host = dockerIp;
                                }
                            }
                        }
                        String jolokiaUrl = "http://" + host + ":" + hostPort + "/jolokia/";
                        return createJolokiaClient(container, jolokiaUrl);
                    }
                }
            }
        }
        return null;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    protected J4pClient createJolokiaClient(ManifestContainer container, String jolokiaUrl) {
        String name = container.getName();
        LOG.info("Creating jolokia client for : " + name + " at URL: " + jolokiaUrl);
        J4pClientBuilder builder = J4pClient.url(jolokiaUrl);
        if (Strings.isNotBlank(user)) {
            builder = builder.user(user);
        }
        if (Strings.isNotBlank(password)) {
            builder = builder.password(password);
        }
        return builder.build();
    }
}