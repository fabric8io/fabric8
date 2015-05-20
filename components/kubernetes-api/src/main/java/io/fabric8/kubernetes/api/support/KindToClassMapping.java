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

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.base.Status;
import io.fabric8.kubernetes.api.model.base.StatusCause;
import io.fabric8.kubernetes.api.model.base.StatusDetails;
import io.fabric8.kubernetes.api.model.errors.StatusError;
import io.fabric8.kubernetes.api.model.resource.Quantity;
import io.fabric8.kubernetes.api.model.util.IntOrString;
import io.fabric8.openshift.api.model.*;
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
        map.put("BuildParameters", Build.class);
        map.put("BuildSource", BuildSource.class);
        map.put("BuildStrategy", BuildStrategy.class);
        map.put("BuildTriggerPolicy", BuildTriggerPolicy.class);
        map.put("Capabilities", Capabilities.class);
        map.put("Container", Container.class);
        map.put("ContainerPort", ContainerPort.class);
        map.put("ContainerState", ContainerState.class);
        map.put("ContainerStateRunning", ContainerStateRunning.class);
        map.put("ContainerStateTerminated", ContainerStateTerminated.class);
        map.put("ContainerStateWaiting", ContainerStateWaiting.class);
        map.put("ContainerStatus", ContainerStatus.class);
        map.put("CustomBuildStrategy", CustomBuildStrategy.class);
        map.put("CustomDeploymentStrategyParams", CustomDeploymentStrategyParams.class);
        map.put("DeploymentCause", DeploymentCause.class);
        map.put("DeploymentCauseImageTrigger", DeploymentCauseImageTrigger.class);
        map.put("DeploymentConfig", DeploymentConfig.class);
        map.put("DeploymentConfigList", DeploymentConfigList.class);
        map.put("DeploymentDetails", DeploymentDetails.class);
        map.put("DeploymentStrategy", DeploymentStrategy.class);
        map.put("DeploymentTriggerImageChangeParams", DeploymentTriggerImageChangeParams.class);
        map.put("DeploymentTriggerPolicy", DeploymentTriggerPolicy.class);
        map.put("DockerBuildStrategy", DockerBuildStrategy.class);
        map.put("EmptyDirVolumeSource", EmptyDirVolumeSource.class);
        map.put("EndpointAddress", EndpointAddress.class);
        map.put("ObjectReference", ObjectReference.class);
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
        map.put("ImageStream", ImageStream.class);
        map.put("ImageStreamList", ImageStreamList.class);
        map.put("ImageStreamStatus", ImageStreamStatus.class);
        map.put("IntOrString", IntOrString.class);
        map.put("KubeSchema", KubeSchema.class);
        map.put("Lifecycle", Lifecycle.class);
        map.put("LifecycleHook", LifecycleHook.class);
        map.put("List", KubernetesList.class);
        map.put("Probe", Probe.class);
        map.put("Node", Node.class);
        map.put("NodeList", NodeList.class);
        map.put("NFSVolumeSource", NFSVolumeSource.class);
        map.put("NamedTagEventList", NamedTagEventList.class);
        map.put("NodeAddress", NodeAddress.class);
        map.put("NodeCondition", NodeCondition.class);
        map.put("NodeStatus", NodeStatus.class);
        map.put("NodeSystemInfo", NodeSystemInfo.class);
        map.put("ObjectReference", ObjectReference.class);
        map.put("Parameter", Parameter.class);
        map.put("Pod", Pod.class);
        map.put("PodCondition", PodCondition.class);
        map.put("PodList", PodList.class);
        map.put("PodStatus", PodStatus.class);
        map.put("PodSpec", PodSpec.class);
        map.put("Quantity", Quantity.class);
        map.put("RecreateDeploymentStrategyParams", RecreateDeploymentStrategyParams.class);
        map.put("ReplicationController", ReplicationController.class);
        map.put("ReplicationControllerList", ReplicationControllerList.class);
        map.put("ReplicationControllerStatus", ReplicationControllerStatus.class);
        map.put("ResourceRequirements", ResourceRequirements.class);
        map.put("Route", Route.class);
        map.put("RouteList", RouteList.class);
        map.put("SourceBuildStrategy", SourceBuildStrategy.class);
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
        map.put("WebHookTrigger", WebHookTrigger.class);
    }

    public static Map<String,Class<?>> getKindToClassMap() {
        return map;
    }
}

