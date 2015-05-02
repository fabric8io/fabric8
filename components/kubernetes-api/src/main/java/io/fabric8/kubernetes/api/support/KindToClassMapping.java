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
package io.fabric8.kubernetes.api.support;


import io.fabric8.kubernetes.api.model.Capabilities;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerManifest;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateRunning;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStateWaiting;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSource;
import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointObjectReference;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.ExecAction;
import io.fabric8.kubernetes.api.model.GCEPersistentDiskVolumeSource;
import io.fabric8.kubernetes.api.model.GitRepoVolumeSource;
import io.fabric8.kubernetes.api.model.GlusterfsVolumeSource;
import io.fabric8.kubernetes.api.model.HTTPGetAction;
import io.fabric8.kubernetes.api.model.Handler;
import io.fabric8.kubernetes.api.model.HostPathVolumeSource;
import io.fabric8.kubernetes.api.model.ISCSIVolumeSource;
import io.fabric8.kubernetes.api.model.KubeSchema;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Lifecycle;
import io.fabric8.kubernetes.api.model.LivenessProbe;
import io.fabric8.kubernetes.api.model.Minion;
import io.fabric8.kubernetes.api.model.MinionList;
import io.fabric8.kubernetes.api.model.NFSVolumeSource;
import io.fabric8.kubernetes.api.model.NodeAddress;
import io.fabric8.kubernetes.api.model.NodeCondition;
import io.fabric8.kubernetes.api.model.NodeResources;
import io.fabric8.kubernetes.api.model.NodeStatus;
import io.fabric8.kubernetes.api.model.NodeSystemInfo;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodState;
import io.fabric8.kubernetes.api.model.PodTemplate;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.ReplicationControllerState;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.RestartPolicy;
import io.fabric8.kubernetes.api.model.RestartPolicyAlways;
import io.fabric8.kubernetes.api.model.RestartPolicyNever;
import io.fabric8.kubernetes.api.model.RestartPolicyOnFailure;
import io.fabric8.kubernetes.api.model.SecretVolumeSource;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.TCPSocketAction;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeSource;
import io.fabric8.kubernetes.api.model.base.Status;
import io.fabric8.kubernetes.api.model.base.StatusCause;
import io.fabric8.kubernetes.api.model.base.StatusDetails;
import io.fabric8.kubernetes.api.model.errors.StatusError;
import io.fabric8.kubernetes.api.model.resource.Quantity;
import io.fabric8.kubernetes.api.model.runtime.RawExtension;
import io.fabric8.kubernetes.api.model.util.IntOrString;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigList;
import io.fabric8.openshift.api.model.BuildList;
import io.fabric8.openshift.api.model.BuildOutput;
import io.fabric8.openshift.api.model.BuildParameters;
import io.fabric8.openshift.api.model.BuildSource;
import io.fabric8.openshift.api.model.BuildStrategy;
import io.fabric8.openshift.api.model.BuildTriggerPolicy;
import io.fabric8.openshift.api.model.CustomBuildStrategy;
import io.fabric8.openshift.api.model.CustomDeploymentStrategyParams;
import io.fabric8.openshift.api.model.Deployment;
import io.fabric8.openshift.api.model.DeploymentCause;
import io.fabric8.openshift.api.model.DeploymentCauseImageTrigger;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigList;
import io.fabric8.openshift.api.model.DeploymentDetails;
import io.fabric8.openshift.api.model.DeploymentList;
import io.fabric8.openshift.api.model.DeploymentStrategy;
import io.fabric8.openshift.api.model.DeploymentTemplate;
import io.fabric8.openshift.api.model.DeploymentTriggerImageChangeParams;
import io.fabric8.openshift.api.model.DeploymentTriggerPolicy;
import io.fabric8.openshift.api.model.DockerBuildStrategy;
import io.fabric8.openshift.api.model.ExecNewPodHook;
import io.fabric8.openshift.api.model.GitBuildSource;
import io.fabric8.openshift.api.model.GitSourceRevision;
import io.fabric8.openshift.api.model.Image;
import io.fabric8.openshift.api.model.ImageChangeTrigger;
import io.fabric8.openshift.api.model.ImageList;
import io.fabric8.openshift.api.model.ImageRepository;
import io.fabric8.openshift.api.model.ImageRepositoryList;
import io.fabric8.openshift.api.model.ImageRepositoryStatus;
import io.fabric8.openshift.api.model.LifecycleHook;
import io.fabric8.openshift.api.model.NamedTagEventList;
import io.fabric8.openshift.api.model.RecreateDeploymentStrategyParams;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteList;
import io.fabric8.openshift.api.model.STIBuildStrategy;
import io.fabric8.openshift.api.model.SourceControlUser;
import io.fabric8.openshift.api.model.SourceRevision;
import io.fabric8.openshift.api.model.TLSConfig;
import io.fabric8.openshift.api.model.TagEvent;
import io.fabric8.openshift.api.model.WebHookTrigger;
import io.fabric8.openshift.api.model.config.Config;
import io.fabric8.openshift.api.model.template.Parameter;
import io.fabric8.openshift.api.model.template.Template;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps the Kubernetes kinds to the Jackson DTO classes
 */
public class KindToClassMapping {
    private static Map<String,Class<?>> map = new HashMap<>();

    static {
        map.put("Build", Build.class);
        map.put("BuildConfig", BuildConfig.class);
        map.put("BuildConfigList", BuildConfigList.class);
        map.put("BuildList", BuildList.class);
        map.put("BuildOutput", BuildOutput.class);
        map.put("BuildParameters", BuildParameters.class);
        map.put("BuildSource", BuildSource.class);
        map.put("BuildStrategy", BuildStrategy.class);
        map.put("BuildTriggerPolicy", BuildTriggerPolicy.class);
        map.put("Capabilities", Capabilities.class);
        map.put("Config", Config.class);
        map.put("Container", Container.class);
        map.put("ContainerManifest", ContainerManifest.class);
        map.put("ContainerPort", ContainerPort.class);
        map.put("ContainerState", ContainerState.class);
        map.put("ContainerStateRunning", ContainerStateRunning.class);
        map.put("ContainerStateTerminated", ContainerStateTerminated.class);
        map.put("ContainerStateWaiting", ContainerStateWaiting.class);
        map.put("ContainerStatus", ContainerStatus.class);
        map.put("CustomBuildStrategy", CustomBuildStrategy.class);
        map.put("CustomDeploymentStrategyParams", CustomDeploymentStrategyParams.class);
        map.put("Deployment", Deployment.class);
        map.put("DeploymentCause", DeploymentCause.class);
        map.put("DeploymentCauseImageTrigger", DeploymentCauseImageTrigger.class);
        map.put("DeploymentConfig", DeploymentConfig.class);
        map.put("DeploymentConfigList", DeploymentConfigList.class);
        map.put("DeploymentDetails", DeploymentDetails.class);
        map.put("DeploymentList", DeploymentList.class);
        map.put("DeploymentStrategy", DeploymentStrategy.class);
        map.put("DeploymentTemplate", DeploymentTemplate.class);
        map.put("DeploymentTriggerImageChangeParams", DeploymentTriggerImageChangeParams.class);
        map.put("DeploymentTriggerPolicy", DeploymentTriggerPolicy.class);
        map.put("DockerBuildStrategy", DockerBuildStrategy.class);
        map.put("EmptyDirVolumeSource", EmptyDirVolumeSource.class);
        map.put("EndpointAddress", EndpointAddress.class);
        map.put("EndpointObjectReference", EndpointObjectReference.class);
        map.put("EndpointPort", EndpointPort.class);
        map.put("EndpointSubset", EndpointSubset.class);
        map.put("Endpoints", Endpoints.class);
        map.put("EndpointsList", EndpointsList.class);
        map.put("EnvVar", EnvVar.class);
        map.put("ExecAction", ExecAction.class);
        map.put("ExecNewPodHook", ExecNewPodHook.class);
        map.put("GCEPersistentDiskVolumeSource", GCEPersistentDiskVolumeSource.class);
        map.put("GitBuildSource", GitBuildSource.class);
        map.put("GitRepoVolumeSource", GitRepoVolumeSource.class);
        map.put("GitSourceRevision", GitSourceRevision.class);
        map.put("GlusterfsVolumeSource", GlusterfsVolumeSource.class);
        map.put("HTTPGetAction", HTTPGetAction.class);
        map.put("Handler", Handler.class);
        map.put("HostPathVolumeSource", HostPathVolumeSource.class);
        map.put("ISCSIVolumeSource", ISCSIVolumeSource.class);
        map.put("Image", Image.class);
        map.put("ImageChangeTrigger", ImageChangeTrigger.class);
        map.put("ImageList", ImageList.class);
        map.put("ImageRepository", ImageRepository.class);
        map.put("ImageRepositoryList", ImageRepositoryList.class);
        map.put("ImageRepositoryStatus", ImageRepositoryStatus.class);
        map.put("IntOrString", IntOrString.class);
        map.put("KubeSchema", KubeSchema.class);
        map.put("KubernetesList", KubernetesList.class);
        map.put("Lifecycle", Lifecycle.class);
        map.put("LifecycleHook", LifecycleHook.class);
        map.put("LivenessProbe", LivenessProbe.class);
        map.put("Minion", Minion.class);
        map.put("MinionList", MinionList.class);
        map.put("NFSVolumeSource", NFSVolumeSource.class);
        map.put("NamedTagEventList", NamedTagEventList.class);
        map.put("NodeAddress", NodeAddress.class);
        map.put("NodeCondition", NodeCondition.class);
        map.put("NodeResources", NodeResources.class);
        map.put("NodeStatus", NodeStatus.class);
        map.put("NodeSystemInfo", NodeSystemInfo.class);
        map.put("ObjectReference", ObjectReference.class);
        map.put("Parameter", Parameter.class);
        map.put("Pod", Pod.class);
        map.put("PodCondition", PodCondition.class);
        map.put("PodList", PodList.class);
        map.put("PodState", PodState.class);
        map.put("PodTemplate", PodTemplate.class);
        map.put("Quantity", Quantity.class);
        map.put("RawExtension", RawExtension.class);
        map.put("RecreateDeploymentStrategyParams", RecreateDeploymentStrategyParams.class);
        map.put("ReplicationController", ReplicationController.class);
        map.put("ReplicationControllerList", ReplicationControllerList.class);
        map.put("ReplicationControllerState", ReplicationControllerState.class);
        map.put("ResourceRequirements", ResourceRequirements.class);
        map.put("RestartPolicy", RestartPolicy.class);
        map.put("RestartPolicyAlways", RestartPolicyAlways.class);
        map.put("RestartPolicyNever", RestartPolicyNever.class);
        map.put("RestartPolicyOnFailure", RestartPolicyOnFailure.class);
        map.put("Route", Route.class);
        map.put("RouteList", RouteList.class);
        map.put("STIBuildStrategy", STIBuildStrategy.class);
        map.put("SecretVolumeSource", SecretVolumeSource.class);
        map.put("Service", Service.class);
        map.put("ServiceList", ServiceList.class);
        map.put("ServicePort", ServicePort.class);
        map.put("SourceControlUser", SourceControlUser.class);
        map.put("SourceRevision", SourceRevision.class);
        map.put("Status", Status.class);
        map.put("StatusCause", StatusCause.class);
        map.put("StatusDetails", StatusDetails.class);
        map.put("StatusError", StatusError.class);
        map.put("TCPSocketAction", TCPSocketAction.class);
        map.put("TLSConfig", TLSConfig.class);
        map.put("TagEvent", TagEvent.class);
        map.put("Template", Template.class);
        map.put("Volume", Volume.class);
        map.put("VolumeMount", VolumeMount.class);
        map.put("VolumeSource", VolumeSource.class);
        map.put("WebHookTrigger", WebHookTrigger.class);
    }

    public static Map<String,Class<?>> getKindToClassMap() {
        return map;
    }
}

