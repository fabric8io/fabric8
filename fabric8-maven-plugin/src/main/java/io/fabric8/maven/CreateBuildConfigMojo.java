/*
 * Copyright 2005-2014 Red Hat, Inc.                                    
 *                                                                      
 * Red Hat licenses this file to you under the Apache License, version  
 * 2.0 (the "License"); you may not use this file except in compliance  
 * with the License.  You may obtain a copy of the License at           
 *                                                                      
 *    http://www.apache.org/licenses/LICENSE-2.0                        
 *                                                                      
 * Unless required by applicable law or agreed to in writing, software  
 * distributed under the License is distributed on an "AS IS" BASIS,    
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or      
 * implied.  See the License for the specific language governing        
 * permissions and limitations under the License.
 */
package io.fabric8.maven;

import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.ServiceNames;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.utils.Strings;
import io.fabric8.utils.URLUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates an OpenShift BuildConfig object for a
 */
@Mojo(name = "create-build-config", requiresProject = false)
public class CreateBuildConfigMojo extends AbstractNamespacedMojo {

    /**
     *
     */
    @Parameter(property = "fabric8.username")
    protected String username;

    /**
     *
     */
    @Parameter(property = "fabric8.repoName")
    protected String repoName;

    /**
     *
     */
    @Parameter(property = "fabric8.fullName")
    protected String fullName;

    /**
     *
     */
    @Parameter(property = "fabric8.gitUrl")
    protected String gitUrl;

    /**
     * The webhook secret used for generic and github webhooks
     */
    @Parameter(property = "fabric8.webhookSecret", defaultValue = "secret101")
    protected  String secret;

    /**
     * the build image stream name
     */
    @Parameter(property = "fabric8.buildImageStream", defaultValue = "triggerJenkins")
    protected  String buildImageStream;

    /**
     * the build image stream tag
     */
    @Parameter(property = "fabric8.buildImageTag", defaultValue = "latest")
    protected  String buildImageTag;

    /**
     * The name of the jenkins job to link to as the first job in the pipeline
     */
    @Parameter(property = "fabric8.jenkinsJob")
    protected  String jenkinsJob;

    /**
     * The name of the jenkins monitor view
     */
    @Parameter(property = "fabric8.jenkinsMonitorView")
    protected  String jenkinsMonitorView;

    /**
     * The name of the jenkins pipline view
     */
    @Parameter(property = "fabric8.jenkinsPipelineView")
    protected  String jenkinsPipelineView;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        KubernetesClient kubernetes = getKubernetes();

        String name = repoName;
        if (Strings.isNotBlank(username)) {
            name = username + "-" + name;
        }
        Map<String,String> labels = new HashMap<>();
        labels.put("user", username);
        labels.put("repo", repoName);

        Map<String,String> annotations = new HashMap<>();
        try {
            String jenkinsUrl = kubernetes.getServiceURL(ServiceNames.JENKINS, kubernetes.getNamespace(), "http", true);

            if (Strings.isNotBlank(jenkinsUrl)) {
                if (Strings.isNotBlank(jenkinsMonitorView)) {
                    annotations.put("fabric8.link.jenkins.monitor/url", URLUtils.pathJoin(jenkinsUrl, "/view", jenkinsMonitorView));
                    annotations.put("fabric8.link.jenkins.monitor/label", "Monitor");
                }
                if (Strings.isNotBlank(jenkinsPipelineView)) {
                    annotations.put("fabric8.link.jenkins.pipeline/url", URLUtils.pathJoin(jenkinsUrl, "/view", jenkinsPipelineView));
                    annotations.put("fabric8.link.jenkins.pipeline/label", "Pipeline");
                }
                if (Strings.isNotBlank(jenkinsJob)) {
                    annotations.put("fabric8.link.jenkins.job/url", URLUtils.pathJoin(jenkinsUrl, "/job", jenkinsJob));
                    annotations.put("fabric8.link.jenkins.job/label", "Job");
                }
            }
        } catch (Exception e) {
            getLog().warn("Could not find the Jenkins URL!: " + e, e);
        }

        BuildConfig buildConfig = new BuildConfigBuilder().
                withNewMetadata().withName(name).withLabels(labels).withAnnotations(annotations).endMetadata().
                withNewSpec().
                withNewSource().
                withType("Git").withNewGit().withUri(gitUrl).endGit().
                endSource().
                withNewStrategy().
                withType("Docker").withNewDockerStrategy().withNewFrom().withName(buildImageStream + ":" + buildImageTag).endFrom().endDockerStrategy().
                endStrategy().
                addNewTrigger().
                withType("github").withNewGithub().withSecret(secret).endGithub().
                endTrigger().
                addNewTrigger().
                withType("generic").withNewGeneric().withSecret(secret).endGeneric().
                endTrigger().
                endSpec().
                build();

        Controller controller = createController();
        controller.applyBuildConfig(buildConfig, "maven");
        getLog().info("Created build configuration for " + name + " in namespace: " + controller.getNamespace() + " at " + kubernetes.getAddress());

    }


}
