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
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.utils.Strings;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 */
public abstract class AbstractNamespacedMojo extends AbstractMojo  {

    @Parameter(property = "fabric8.namespace")
    protected String namespace;

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
    @Parameter(property = "fabric8.recreate")
    protected boolean recreate;



    private KubernetesClient kubernetes = new KubernetesClient();

    public KubernetesClient getKubernetes() {
        if (Strings.isNotBlank(namespace)) {
            kubernetes.setNamespace(namespace);
        }
        return kubernetes;
    }


    protected Controller createController() {
        Controller controller = new Controller(getKubernetes());
        controller.setThrowExceptionOnError(failOnError);
        controller.setRecreateMode(recreate);
        return controller;
    }

}
