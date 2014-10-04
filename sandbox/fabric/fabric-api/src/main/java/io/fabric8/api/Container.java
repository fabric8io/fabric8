/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.api;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public interface Container extends HasId {

    String PROVISION_SUCCESS = "success";
    String PROVISION_ERROR = "error";
    String PROVISION_FAILED = "failed";
    String PROVISION_STOPPED = "stopped";
    String PROVISION_DOWNLOADING = "downloading";
    String PROVISION_FINALIZING = "finalizing";
    String PROVISION_INSTALLING = "installing";

    String getType();

    /**
     * Allows a container provider to customise the type
     */
    void setType(String type);

    String getId();

    Container getParent();

    FabricService getFabricService();

    boolean isAlive();

    /**
     * Allows the alive nature to be set by a remote monitoring process. Usually an agent does this itself
     */
    void setAlive(boolean flag);

    boolean isEnsembleServer();

    // Runtime informations
    boolean isRoot();
    String getSshUrl();
    String getJmxUrl();
    String getHttpUrl();

    String getJolokiaUrl();
    void setJolokiaUrl(String location);

    /**
     * Returns the debugging port text for this container or null if debugging isn't enabled
     */
    String getDebugPort();

    void setHttpUrl(String location);

    boolean isManaged();

    String getVersionId();
    void setVersionId(String versionId);
    
    Version getVersion();
    void setVersion(Version version);

    Long getProcessId();

    Profile[] getProfiles();
    List<String> getProfileIds();
    void setProfiles(Profile[] profiles);
    void addProfiles(Profile... profiles);
    void removeProfiles(String... profileIds);

    Profile getOverlayProfile();

    String getLocation();
    void setLocation(String location);

    String getGeoLocation();
    void setGeoLocation(String geoLocation);

    /**
     * Returns the resolver of the {@link Container}.
     * The resolver identifies which of the {@link Container} address should be used for address resolution.
     * @return One of the: localip, localhostname, publicip, publichostname, manualip.
     */
    String getResolver();

    /**
     * Sets the resolver value of the {@link Container}.
     * @param resolver
     */
    void setResolver(String resolver);

    /**
     * Returns the resolved address of the {@link Container}.
     * @return
     */
    String getIp();

    String getLocalIp();
    void setLocalIp(String localIp);

    String getLocalHostname();
    void setLocalHostname(String localHostname);

    String getPublicIp();
    void setPublicIp(String publicIp);

    String getPublicHostname();
    void setPublicHostname(String publicHostname);

    String getManualIp();
    void setManualIp(String manualIp);

    int getMinimumPort();
    void setMinimumPort(int port);

    int getMaximumPort();
    void setMaximumPort(int port);



    void start();
    void start(boolean force);
    void stop();
    void stop(boolean force);
    void destroy();
    void destroy(boolean force);

    //  gets children containers, eg process instances, maybe camel contexts
    Container[] getChildren();

    List<String> getJmxDomains();

    /**
     * Allows the JMX domains to be updated by a remote monitoring process; usually these are updated by an agent inside the JVM.
     */
    void setJmxDomains(List<String> jmxDomains);

    //BundleInfo[] getBundles(ContainerTemplate template);

    //ServiceInfo[] getServices(ContainerTemplate template);

    /**
     * Returns true if the initial provisioning of the container is complete so that we can connect to it
     * via SSH / JMX etc (e.g. the ZK ensemble is joined and the security realm is in place).
     */
    boolean isProvisioningComplete();

    /**
     * Returns true if the container is being provisioned and it is not yet complete (i.e. its not succeeded or failed)
     */
    boolean isProvisioningPending();

    String getProvisionResult();
    void setProvisionResult(String result);

    String getProvisionException();
    void setProvisionException(String exception);

    List<String> getProvisionList();
    void setProvisionList(List<String> bundles);

    Properties getProvisionChecksums();
    void setProvisionChecksums(Properties checksums);

    String getProvisionStatus();

    Map<String, String> getProvisionStatusMap();

    CreateContainerMetadata<?> getMetadata();


    /**
     * Returns true if the container is alive and provisioning is successful (if required)
     */
    boolean isAliveAndOK();
}
