/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Arguments for creating a new container via JClouds
 */
public class CreateJCloudsContainerOptions extends CreateContainerBasicOptions<CreateJCloudsContainerOptions> implements CreateRemoteContainerOptions<CreateJCloudsContainerOptions> {
    private static final long serialVersionUID = 4489740280396972109L;

    private String osFamily;
    private String osVersion;
    private String imageId;
    private String hardwareId;
    private String locationId;
    private String group;
    private String user;
    private String password;
    private String contextName;
    private String providerName;
    private String apiName;
    private String endpoint;
    private JCloudsInstanceType instanceType = JCloudsInstanceType.Fastest;
    private String identity;
    private String credential;
    private String owner;
    private final Map<String, String> serviceOptions = new HashMap<String, String>();
    private final Map<String, String> nodeOptions = new HashMap<String, String>();
    private int servicePort = 0;
    private String publicKeyFile;
    private transient Object computeService;
    private CreateEnsembleOptions createEnsembleOptions = CreateEnsembleOptions.build();
    private String path  = "~/containers/";

    public CreateJCloudsContainerOptions() {
        this.providerType = "jclouds";
        this.adminAccess = true;
    }

    @Override
    public String toString() {
        return "CreateJCloudsContainerArguments{" +
                "imageId='" + imageId + '\'' +
                ", hardwareId='" + hardwareId + '\'' +
                ", locationId='" + locationId + '\'' +
                ", group='" + group + '\'' +
                ", user='" + user + '\'' +
                ", instanceType='" + instanceType + '\'' +
                '}';
    }

    public CreateJCloudsContainerOptions osVersion(final String osVersion) {
        this.osVersion = osVersion;
        return this;
    }

    public CreateJCloudsContainerOptions osFamily(final String osFamily) {
        this.osFamily = osFamily;
        return this;
    }

    public CreateJCloudsContainerOptions imageId(final String imageId) {
        this.imageId = imageId;
        return this;
    }

    public CreateJCloudsContainerOptions hardwareId(final String hardwareId) {
        this.hardwareId = hardwareId;
        return this;
    }

    public CreateJCloudsContainerOptions locationId(final String locationId) {
        this.locationId = locationId;
        return this;
    }

    public CreateJCloudsContainerOptions group(final String group) {
        this.group = group;
        return this;
    }

    public CreateJCloudsContainerOptions user(final String user) {
        this.user = user;
        return this;
    }


    public CreateJCloudsContainerOptions password(final String password) {
        this.password = password;
        return this;
    }

    public CreateJCloudsContainerOptions contextName(final String contextName) {
        this.contextName = contextName;
        return this;
    }
    public CreateJCloudsContainerOptions providerName(final String providerName) {
        this.providerName = providerName;
        return this;
    }

    public CreateJCloudsContainerOptions apiName(final String apiName) {
        this.apiName = apiName;
        return this;
    }

    public CreateJCloudsContainerOptions endpoint(final String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public CreateJCloudsContainerOptions instanceType(final JCloudsInstanceType instanceType) {
        this.instanceType = instanceType;
        return this;
    }

    public CreateJCloudsContainerOptions identity(final String identity) {
        this.identity = identity;
        return this;
    }

    public CreateJCloudsContainerOptions credential(final String credential) {
        this.credential = credential;
        return this;
    }

    /**
     * Use servie options instead.
     * @param owner
     * @return
     */
    @Deprecated
    public CreateJCloudsContainerOptions owner(final String owner) {
        this.owner = owner;
        if (owner != null) {
            this.serviceOptions.put("owner", owner);
        }
        return this;
    }

    public CreateJCloudsContainerOptions nodeOptions(final Map<String, String> nodeOptions) {
        if (nodeOptions != null) {
            for (Map.Entry<String, String> entry : nodeOptions.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                this.nodeOptions.put(key,value);
            }
        }
        return this;
    }

    public CreateJCloudsContainerOptions serviceOptions(final Map<String, String> serviceOptions) {
        if (serviceOptions != null) {
            for (Map.Entry<String, String> entry : serviceOptions.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                this.serviceOptions.put(key,value);
            }
        }
        return this;
    }


    public CreateJCloudsContainerOptions servicePort(int servicePort) {
        this.servicePort = servicePort;
        return this;
    }

    public CreateJCloudsContainerOptions publicKeyFile(final String publicKeyFile) {
        this.publicKeyFile = publicKeyFile;
        return this;
    }

    public CreateJCloudsContainerOptions createEnsembleOptions(final CreateEnsembleOptions createEnsembleOptions) {
        this.createEnsembleOptions = createEnsembleOptions;
        return this;
    }

    @Override
    public CreateJCloudsContainerOptions path(String path) {
        this.path = path;
        return this;
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
         this.imageId = imageId;
    }

    public String getHardwareId() {
        return hardwareId;
    }

    public void setHardwareId(String hardwareId) {
        this.hardwareId = hardwareId;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
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

    public String getContextName() {
        return contextName;
    }

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public JCloudsInstanceType getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(JCloudsInstanceType instanceType) {
        this.instanceType = instanceType;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public int getServicePort() {
        return servicePort;
    }

    public void setServicePort(Integer servicePort) {
        this.servicePort = servicePort;
    }

    public String getOsFamily() {
        return osFamily;
    }

    public void setOsFamily(String osFamily) {
        this.osFamily = osFamily;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getPublicKeyFile() {
        return publicKeyFile;
    }

    public void setPublicKeyFile(String publicKeyFile) {
        this.publicKeyFile = publicKeyFile;
    }

    public Map<String, String> getServiceOptions() {
        return serviceOptions;
    }

    public Map<String, String> getNodeOptions() {
        return nodeOptions;
    }
    public Object getComputeService() {
        return computeService;
    }

    /**
     * Sets an optional compute service to use for the creation of the container
     */
    public void setComputeService(Object computeService) {
        this.computeService = computeService;
    }

    public CreateEnsembleOptions getCreateEnsembleOptions() {
        return createEnsembleOptions;
    }

    public void setCreateEnsembleOptions(CreateEnsembleOptions createEnsembleOptions) {
        this.createEnsembleOptions = createEnsembleOptions;
    }

	public CreateJCloudsContainerOptions clone() throws CloneNotSupportedException {
		return (CreateJCloudsContainerOptions) super.clone();
	}
}
