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
package io.fabric8.kubernetes.api.support;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.openshift.api.model.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps the Kubernetes kinds to the Jackson DTO classes
 */
public class KindToClassMapping {
    private static Map<String,Class<?>> map = new HashMap<>();

    static {
        map.put("AWSElasticBlockStoreVolumeSource", AWSElasticBlockStoreVolumeSource.class);
        map.put("AuthInfo", AuthInfo.class);
        map.put("BaseKubernetesList", BaseKubernetesList.class);
        map.put("Build", Build.class);
        map.put("BuildConfig", BuildConfig.class);
        map.put("BuildConfigList", BuildConfigList.class);
        map.put("BuildConfigSpec", BuildConfigSpec.class);
        map.put("BuildConfigStatus", BuildConfigStatus.class);
        map.put("BuildList", BuildList.class);
        map.put("BuildOutput", BuildOutput.class);
        map.put("BuildSource", BuildSource.class);
        map.put("BuildSpec", BuildSpec.class);
        map.put("BuildStatus", BuildStatus.class);
        map.put("BuildStrategy", BuildStrategy.class);
        map.put("BuildTriggerPolicy", BuildTriggerPolicy.class);
        map.put("Capabilities", Capabilities.class);
        map.put("Cluster", Cluster.class);
        map.put("Config", Config.class);
        map.put("Container", Container.class);
        map.put("ContainerPort", ContainerPort.class);
        map.put("ContainerState", ContainerState.class);
        map.put("ContainerStateRunning", ContainerStateRunning.class);
        map.put("ContainerStateTerminated", ContainerStateTerminated.class);
        map.put("ContainerStateWaiting", ContainerStateWaiting.class);
        map.put("ContainerStatus", ContainerStatus.class);
        map.put("Context", Context.class);
        map.put("CustomBuildStrategy", CustomBuildStrategy.class);
        map.put("CustomDeploymentStrategyParams", CustomDeploymentStrategyParams.class);
        map.put("DeploymentCause", DeploymentCause.class);
        map.put("DeploymentCauseImageTrigger", DeploymentCauseImageTrigger.class);
        map.put("DeploymentConfig", DeploymentConfig.class);
        map.put("DeploymentConfigList", DeploymentConfigList.class);
        map.put("DeploymentConfigSpec", DeploymentConfigSpec.class);
        map.put("DeploymentConfigStatus", DeploymentConfigStatus.class);
        map.put("DeploymentDetails", DeploymentDetails.class);
        map.put("DeploymentStrategy", DeploymentStrategy.class);
        map.put("DeploymentTriggerImageChangeParams", DeploymentTriggerImageChangeParams.class);
        map.put("DeploymentTriggerPolicy", DeploymentTriggerPolicy.class);
        map.put("DockerBuildStrategy", DockerBuildStrategy.class);
        map.put("EmptyDirVolumeSource", EmptyDirVolumeSource.class);
        map.put("EndpointAddress", EndpointAddress.class);
        map.put("EndpointPort", EndpointPort.class);
        map.put("EndpointSubset", EndpointSubset.class);
        map.put("Endpoints", Endpoints.class);
        map.put("EndpointsList", EndpointsList.class);
        map.put("EnvVar", EnvVar.class);
        map.put("EnvVarSource", EnvVarSource.class);
        map.put("ExecAction", ExecAction.class);
        map.put("ExecNewPodHook", ExecNewPodHook.class);
        map.put("GCEPersistentDiskVolumeSource", GCEPersistentDiskVolumeSource.class);
        map.put("GitBuildSource", GitBuildSource.class);
        map.put("GitRepoVolumeSource", GitRepoVolumeSource.class);
        map.put("GitSourceRevision", GitSourceRevision.class);
        map.put("GlusterfsVolumeSource", GlusterfsVolumeSource.class);
        map.put("HTTPGetAction", HTTPGetAction.class);
        map.put("Handler", Handler.class);
        map.put("HasMetadata", HasMetadata.class);
        map.put("HostPathVolumeSource", HostPathVolumeSource.class);
        map.put("ISCSIVolumeSource", ISCSIVolumeSource.class);
        map.put("Image", Image.class);
        map.put("ImageChangeTrigger", ImageChangeTrigger.class);
        map.put("ImageList", ImageList.class);
        map.put("ImageStream", ImageStream.class);
        map.put("ImageStreamList", ImageStreamList.class);
        map.put("ImageStreamSpec", ImageStreamSpec.class);
        map.put("ImageStreamStatus", ImageStreamStatus.class);
        map.put("IntOrString", IntOrString.class);
        map.put("KubeSchema", KubeSchema.class);
        map.put("KubernetesList", KubernetesList.class);
        map.put("Lifecycle", Lifecycle.class);
        map.put("LifecycleHook", LifecycleHook.class);
        map.put("ListMeta", ListMeta.class);
        map.put("NFSVolumeSource", NFSVolumeSource.class);
        map.put("NamedAuthInfo", NamedAuthInfo.class);
        map.put("NamedCluster", NamedCluster.class);
        map.put("NamedContext", NamedContext.class);
        map.put("NamedExtension", NamedExtension.class);
        map.put("NamedTagEventList", NamedTagEventList.class);
        map.put("Namespace", Namespace.class);
        map.put("NamespaceList", NamespaceList.class);
        map.put("NamespaceSpec", NamespaceSpec.class);
        map.put("NamespaceStatus", NamespaceStatus.class);
        map.put("Node", Node.class);
        map.put("NodeAddress", NodeAddress.class);
        map.put("NodeCondition", NodeCondition.class);
        map.put("NodeList", NodeList.class);
        map.put("NodeSpec", NodeSpec.class);
        map.put("NodeStatus", NodeStatus.class);
        map.put("NodeSystemInfo", NodeSystemInfo.class);
        map.put("OAuthAccessToken", OAuthAccessToken.class);
        map.put("OAuthAccessTokenList", OAuthAccessTokenList.class);
        map.put("OAuthAuthorizeToken", OAuthAuthorizeToken.class);
        map.put("OAuthAuthorizeTokenList", OAuthAuthorizeTokenList.class);
        map.put("OAuthClient", OAuthClient.class);
        map.put("OAuthClientAuthorization", OAuthClientAuthorization.class);
        map.put("OAuthClientAuthorizationList", OAuthClientAuthorizationList.class);
        map.put("OAuthClientList", OAuthClientList.class);
        map.put("ObjectFieldSelector", ObjectFieldSelector.class);
        map.put("ObjectMeta", ObjectMeta.class);
        map.put("ObjectReference", ObjectReference.class);
        map.put("Parameter", Parameter.class);
        map.put("PersistentVolumeClaimVolumeSource", PersistentVolumeClaimVolumeSource.class);
        map.put("Pod", Pod.class);
        map.put("PodCondition", PodCondition.class);
        map.put("PodList", PodList.class);
        map.put("PodSpec", PodSpec.class);
        map.put("PodStatus", PodStatus.class);
        map.put("PodTemplateSpec", PodTemplateSpec.class);
        map.put("Preferences", Preferences.class);
        map.put("Probe", Probe.class);
        map.put("Quantity", Quantity.class);
        map.put("RecreateDeploymentStrategyParams", RecreateDeploymentStrategyParams.class);
        map.put("ReplicationController", ReplicationController.class);
        map.put("ReplicationControllerList", ReplicationControllerList.class);
        map.put("ReplicationControllerSpec", ReplicationControllerSpec.class);
        map.put("ReplicationControllerStatus", ReplicationControllerStatus.class);
        map.put("ResourceRequirements", ResourceRequirements.class);
        map.put("RollingDeploymentStrategyParams", RollingDeploymentStrategyParams.class);
        map.put("Route", Route.class);
        map.put("RouteList", RouteList.class);
        map.put("SourceBuildStrategy", SourceBuildStrategy.class);
        map.put("RouteSpec", RouteSpec.class);
        map.put("RouteStatus", RouteStatus.class);
        map.put("SELinuxOptions", SELinuxOptions.class);
        map.put("Secret", Secret.class);
        map.put("SecretList", SecretList.class);
        map.put("SecretVolumeSource", SecretVolumeSource.class);
        map.put("SecurityContext", SecurityContext.class);
        map.put("Service", Service.class);
        map.put("ServiceAccount", ServiceAccount.class);
        map.put("ServiceAccountList", ServiceAccountList.class);
        map.put("ServiceList", ServiceList.class);
        map.put("ServicePort", ServicePort.class);
        map.put("ServiceSpec", ServiceSpec.class);
        map.put("ServiceStatus", ServiceStatus.class);
        map.put("SourceBuildStrategy", SourceBuildStrategy.class);
        map.put("SourceControlUser", SourceControlUser.class);
        map.put("SourceRevision", SourceRevision.class);
        map.put("Status", Status.class);
        map.put("StatusCause", StatusCause.class);
        map.put("StatusDetails", StatusDetails.class);
        map.put("TCPSocketAction", TCPSocketAction.class);
        map.put("TLSConfig", TLSConfig.class);
        map.put("TagEvent", TagEvent.class);
        map.put("Template", Template.class);
        map.put("Volume", Volume.class);
        map.put("VolumeMount", VolumeMount.class);
        map.put("WebHookTrigger", WebHookTrigger.class);
    }

    public static Map<String,Class<?>> getKindToClassMap() {
        return map;
    }
}

