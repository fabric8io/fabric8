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

import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Strings;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractNamespacedMojo extends AbstractMojo  {

    @Parameter(property = "fabric8.namespace")
    private String namespace = KubernetesHelper.defaultNamespace();

    /**
     * The domain added to the service ID when creating OpenShift routes
     */
    @Parameter(property = "fabric8.domain", defaultValue = "${env.KUBERNETES_DOMAIN}")
    protected String routeDomain;

    /**
     * Should we fail the build if an apply fails?
     */
    @Parameter(property = "fabric8.apply.failOnError", defaultValue = "true")
    protected boolean failOnError;

    /**
     * Should we update resources by deleting them first and then creating them again?
     */
    @Parameter(property = "fabric8.recreate", defaultValue = "false")
    protected boolean recreate;

    private KubernetesClient kubernetes;

    public KubernetesClient getKubernetes() {
        return new DefaultKubernetesClient();
    }

    protected Controller createController() {
        Controller controller = new Controller(getKubernetes());
        controller.setThrowExceptionOnError(failOnError);
        controller.setRecreateMode(recreate);
        getLog().debug("Using recreate mode: " + recreate);
        return controller;
    }

    protected synchronized String getNamespace() {
        if (Strings.isNullOrBlank(namespace)) {
            namespace = KubernetesHelper.defaultNamespace();
        }
        return namespace;
    }

    public String getRouteDomain() {
        return routeDomain;
    }

    public void setRouteDomain(String routeDomain) {
        this.routeDomain = routeDomain;
    }

    public boolean isRecreate() {
        return recreate;
    }

    public void setRecreate(boolean recreate) {
        this.recreate = recreate;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }
}
