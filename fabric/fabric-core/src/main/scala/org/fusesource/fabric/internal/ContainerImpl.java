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
package org.fusesource.fabric.internal;

import org.apache.zookeeper.KeeperException;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.CreateContainerMetadata;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.api.data.BundleInfo;
import org.fusesource.fabric.api.data.ServiceInfo;
import org.fusesource.fabric.service.ContainerTemplate;
import org.fusesource.fabric.service.FabricServiceImpl;
import org.fusesource.fabric.utils.Base64Encoder;
import org.fusesource.fabric.utils.ObjectUtils;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;
import org.osgi.jmx.framework.BundleStateMBean;
import org.osgi.jmx.framework.ServiceStateMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ContainerImpl implements Container {

    private static final String ENSEMBLE_PROFILE_PATTERN = "fabric-ensemble-[0-9]*-[0-9]*";

    /**
     * Logger.
     */
    protected transient Logger logger = LoggerFactory.getLogger(getClass());

    private final Container parent;
    private final String id;
    private final FabricServiceImpl service;
    private CreateContainerMetadata<?> metadata;

    public ContainerImpl(Container parent, String id, FabricServiceImpl service) {
        this.parent = parent;
        this.id = id;
        this.service = service;
    }

    public Container getParent() {
        return parent;
    }

    public String getId() {
        return id;
    }

    public boolean isAlive() {
        try {
            return service.getZooKeeper().exists(ZkPath.CONTAINER_ALIVE.getPath(id)) != null;
        } catch (KeeperException.NoNodeException e) {
            return false;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public boolean isRoot() {
        return parent == null;
    }

    @Override
    public boolean isEnsembleServer() {
        try {
            String version = getVersion().getName();
            String clusterId = service.getZooKeeper().getStringData("/fabric/configs/versions/" + version + "/general/fabric-ensemble");
            String containers = service.getZooKeeper().getStringData("/fabric/configs/versions/" + version + "/general/fabric-ensemble/" + clusterId);
            for (String name : containers.split(",")) {
                if (id.equals(name)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public boolean isProvisioningComplete() {
        String result = getProvisionResult();
        return ZkDefs.SUCCESS.equals(result) || ZkDefs.ERROR.equals(result);
    }

    @Override
    public boolean isProvisioningPending() {
        String result = getProvisionResult();
        if (result == null) {
            return false;
        } else {
            return !isProvisioningComplete();
        }
    }

    @Override
    public String getProvisionStatus() {
        String provisioned = getProvisionResult();
        String provisionException = getProvisionException();
        String result = "not provisioned";

        if (provisioned != null) {
            result = provisioned;
            if (result.equals(ZkDefs.ERROR) && provisionException != null) {
                result += " - " + provisionException.split(System.getProperty("line.separator"))[0];
            }
        }
        return result;
    }

    public String getSshUrl() {
        try {
            return ZooKeeperUtils.getSubstitutedPath(service.getZooKeeper(), ZkPath.CONTAINER_SSH.getPath(id));
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public String getJmxUrl() {
        try {
            return ZooKeeperUtils.getSubstitutedPath(service.getZooKeeper(), ZkPath.CONTAINER_JMX.getPath(id));
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    private String getZkData(ZkPath path) {
        try {
            return service.getZooKeeper().getStringData(path.getPath(id));
        } catch (KeeperException.NoNodeException e) {
            return null;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public boolean isManaged() {
        return getProvisionResult() != null;
    }

    @Override
    public Version getVersion() {
        try {
            String version = getZkData(ZkPath.CONFIG_CONTAINER);
            if (version == null) {
                return null;
            }
            return new VersionImpl(version, service);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setVersion(Version version) {
        try {
            Version curretVersion = getVersion();

            Profile[] profiles = getProfiles();
            if (requiresUpgrade(version) && isManaged()) {
                if (version.compareTo(curretVersion) > 0) {
                    ZooKeeperUtils.set(service.getZooKeeper(), ZkPath.CONTAINER_PROVISION_RESULT.getPath(getId()), "upgrading");
                } else {
                    ZooKeeperUtils.set(service.getZooKeeper(), ZkPath.CONTAINER_PROVISION_RESULT.getPath(getId()), "downgrading");
                }
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < profiles.length; i++) {
                if (i > 0) {
                    sb.append(" ");
                }
                sb.append(profiles[i].getId());
            }

            //Transfer profiles to the new version.
            ZooKeeperUtils.set(service.getZooKeeper(), ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version.getName(), id), sb.toString());
            ZooKeeperUtils.set(service.getZooKeeper(), ZkPath.CONFIG_CONTAINER.getPath(id), version.getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Profile[] getProfiles() {
        try {
            String version = service.getZooKeeper().getStringData(ZkPath.CONFIG_CONTAINER.getPath(id));
            String node = ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, id);
            String str = service.getZooKeeper().getStringData(node);
            if (str == null) {
                return new Profile[0];
            }
            List<Profile> profiles = new ArrayList<Profile>();
            if (!str.trim().isEmpty()) {
                for (String p : str.split(" +")) {
                    if (!p.isEmpty()) {
                        profiles.add(new ProfileImpl(p, version, service));
                    }
                }
            }
            if (profiles.isEmpty()) {
                profiles.add(new ProfileImpl("default", version, service));
            }
            return profiles.toArray(new Profile[profiles.size()]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setProfiles(Profile[] profiles) {
        try {
            String version = service.getZooKeeper().getStringData(ZkPath.CONFIG_CONTAINER.getPath(id));
            String node = ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, id);
            List<String> existingProfiles = Arrays.asList(service.getZooKeeper().getStringData(node).split(" "));

            StringBuilder sb = new StringBuilder();
            if (profiles != null) {
                for (Profile profile : profiles) {
                    if (!version.equals(profile.getVersion())) {
                        throw new IllegalArgumentException("Version mismatch setting profile " + profile.getId() + " with version "
                                + profile.getVersion() + " expected version " + version);
                    } else if (profile.isAbstract()) {
                        throw new IllegalArgumentException("The profile " + profile.getId() + " is abstract and can not "
                                + "be associated to containers");
                    } else if (profile.getId().matches(ENSEMBLE_PROFILE_PATTERN) && !existingProfiles.contains(profile.getId())) {
                        throw new IllegalArgumentException("The profile " + profile.getId() + " is not assignable.");
                    }

                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append(profile.getId());
                }
            }
            String str = sb.toString();
            if (str.trim().isEmpty()) {
                str = "default";
            }
            service.getZooKeeper().setData(node, str);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public String getLocation() {
        try {
            String path = ZkPath.CONTAINER_LOCATION.getPath(id);
            if (service.getZooKeeper().exists(path) != null) {
                return service.getZooKeeper().getStringData(path);
            } else {
                return "";
            }
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public void setLocation(String location) {
        try {
            String path = ZkPath.CONTAINER_LOCATION.getPath(id);
            ZooKeeperUtils.set(service.getZooKeeper(), path, location);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public String getGeoLocation() {
        try {
            String path = ZkPath.CONTAINER_GEOLOCATION.getPath(id);
            if (service.getZooKeeper().exists(path) != null) {
                return service.getZooKeeper().getStringData(path);
            } else {
                return "";
            }
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public void setGeoLocation(String location) {
        try {
            String path = ZkPath.CONTAINER_GEOLOCATION.getPath(id);
            ZooKeeperUtils.set(service.getZooKeeper(), path, location);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    /**
     * Returns the resolver of the {@link org.fusesource.fabric.api.Container}.
     * The resolver identifies which of the {@link org.fusesource.fabric.api.Container} address should be used for address resolution.
     *
     * @return One of the: localip, localhostname, publicip, publichostname, manualip.
     */
    @Override
    public String getResolver() {
        try {
            return ZooKeeperUtils.getSubstitutedPath(service.getZooKeeper(), ZkPath.CONTAINER_RESOLVER.getPath(id));
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    /**
     * Sets the resolver value of the {@link org.fusesource.fabric.api.Container}.
     *
     * @param resolver
     */
    @Override
    public void setResolver(String resolver) {
        try {
            ZooKeeperUtils.set(service.getZooKeeper(), ZkPath.CONTAINER_IP.getPath(id), "${zk:" + id + "/" + resolver + "}");
            ZooKeeperUtils.set(service.getZooKeeper(), ZkPath.CONTAINER_RESOLVER.getPath(id), resolver);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    /**
     * Returns the resolved address of the {@link org.fusesource.fabric.api.Container}.
     *
     * @return
     */
    @Override
    public String getIp() {
        try {
            return ZooKeeperUtils.getSubstitutedPath(service.getZooKeeper(), ZkPath.CONTAINER_IP.getPath(id));
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public String getLocalIp() {
        try {
            if (service.getZooKeeper().exists(ZkPath.CONTAINER_LOCAL_IP.getPath(id)) == null) {
                return null;
            } else {
                return ZooKeeperUtils.getSubstitutedPath(service.getZooKeeper(), ZkPath.CONTAINER_LOCAL_IP.getPath(id));
            }
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setLocalIp(String localIp) {
        try {
            ZooKeeperUtils.set(service.getZooKeeper(), ZkPath.CONTAINER_LOCAL_IP.getPath(id), localIp);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public String getLocalHostname() {
        try {
            if (service.getZooKeeper().exists(ZkPath.CONTAINER_LOCAL_HOSTNAME.getPath(id)) == null) {
                return null;
            } else {
                return ZooKeeperUtils.getSubstitutedPath(service.getZooKeeper(), ZkPath.CONTAINER_LOCAL_HOSTNAME.getPath(id));
            }
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setLocalHostname(String localHostname) {
        try {
            ZooKeeperUtils.set(service.getZooKeeper(), ZkPath.CONTAINER_LOCAL_HOSTNAME.getPath(id), localHostname);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public String getPublicIp() {
        try {
            if (service.getZooKeeper().exists(ZkPath.CONTAINER_PUBLIC_IP.getPath(id)) == null) {
                return null;
            } else {
                return ZooKeeperUtils.getSubstitutedPath(service.getZooKeeper(), ZkPath.CONTAINER_PUBLIC_IP.getPath(id));
            }
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setPublicIp(String publicIp) {
        try {
            ZooKeeperUtils.set(service.getZooKeeper(), ZkPath.CONTAINER_PUBLIC_IP.getPath(id), publicIp);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public String getPublicHostname() {
        try {
            if (service.getZooKeeper().exists(ZkPath.CONTAINER_PUBLIC_HOSTNAME.getPath(id)) == null) {
                return null;
            } else {
                return ZooKeeperUtils.getSubstitutedPath(service.getZooKeeper(), ZkPath.CONTAINER_PUBLIC_HOSTNAME.getPath(id));
            }
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setPublicHostname(String publicHostname) {
        try {
            ZooKeeperUtils.set(service.getZooKeeper(), ZkPath.CONTAINER_PUBLIC_HOSTNAME.getPath(id), publicHostname);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public String getManulIp() {
        try {
            if (service.getZooKeeper().exists(ZkPath.CONTAINER_MANUAL_IP.getPath(id)) == null) {
                return null;
            } else {
                return ZooKeeperUtils.getSubstitutedPath(service.getZooKeeper(), ZkPath.CONTAINER_MANUAL_IP.getPath(id));
            }
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setManualIp(String manualIp) {
        try {
            ZooKeeperUtils.set(service.getZooKeeper(), ZkPath.CONTAINER_MANUAL_IP.getPath(id), manualIp);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public int getMinimumPort() {
        int minimumPort = 0;
        try {
            if (service.getZooKeeper().exists(ZkPath.CONTAINER_PORT_MIN.getPath(id)) != null) {
                minimumPort = Integer.parseInt(service.getZooKeeper().getStringData(ZkPath.CONTAINER_PORT_MIN.getPath(id)));
            }
        } catch (InterruptedException e) {
            throw new FabricException(e);
        } catch (KeeperException e) {
            throw new FabricException(e);
        } catch (NumberFormatException e) {
            //ignore and fallback to 0
        }
        return minimumPort;
    }

    @Override
    public void setMinimumPort(int port) {
        try {
            ZooKeeperUtils.set(service.getZooKeeper(), ZkPath.CONTAINER_PORT_MIN.getPath(id), String.valueOf(port));
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public int getMaximumPort() {
        int maximumPort = 0;
        try {
            if (service.getZooKeeper().exists(ZkPath.CONTAINER_PORT_MAX.getPath(id)) != null) {
                maximumPort = Integer.parseInt(service.getZooKeeper().getStringData(ZkPath.CONTAINER_PORT_MAX.getPath(id)));
            }
        } catch (InterruptedException e) {
            throw new FabricException(e);
        } catch (KeeperException e) {
            throw new FabricException(e);
        } catch (NumberFormatException e) {
            //ignore and fallback to 0
        }
        return maximumPort;
    }

    @Override
    public void setMaximumPort(int port) {
        try {
            ZooKeeperUtils.set(service.getZooKeeper(), ZkPath.CONTAINER_PORT_MAX.getPath(id), String.valueOf(port));
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public BundleInfo[] getBundles(ContainerTemplate containerTemplate) {
        try {
            return containerTemplate.execute(new ContainerTemplate.BundleStateCallback<BundleInfo[]>() {
                public BundleInfo[] doWithBundleState(BundleStateMBean bundleState) throws Exception {
                    TabularData bundles = bundleState.listBundles();
                    BundleInfo[] info = new BundleInfo[bundles.size()];

                    int i = 0;
                    for (Object data : bundles.values().toArray()) {
                        info[i++] = new JmxBundleInfo((CompositeData) data);
                    }

                    // sort bundles using bundle id to preserve same order like in framework
                    Arrays.sort(info, new BundleInfoComparator());
                    return info;
                }
            });
        } catch (Exception e) {
            logger.warn("Error while retrieving bundles. This exception will be ignored.", e);
            return new BundleInfo[0];
        }
    }

    public ServiceInfo[] getServices(ContainerTemplate containerTemplate) {
        try {
            return containerTemplate.execute(new ContainerTemplate.ServiceStateCallback<ServiceInfo[]>() {
                public ServiceInfo[] doWithServiceState(ServiceStateMBean serviceState) throws Exception {
                    TabularData services = serviceState.listServices();
                    ServiceInfo[] info = new ServiceInfo[services.size()];

                    int i = 0;
                    for (Object data : services.values().toArray()) {
                        CompositeData svc = (CompositeData) data;
                        info[i++] = new JmxServiceInfo(svc, serviceState.getProperties((Long) svc.get(ServiceStateMBean.IDENTIFIER)));
                    }

                    // sort services using service id to preserve same order like in framework
                    Arrays.sort(info, new ServiceInfoComparator());
                    return info;
                }
            });
        } catch (Exception e) {
            logger.warn("Error while retrieving services. This exception will be ignored.", e);
            return new ServiceInfo[0];
        }
    }

    public List<String> getJmxDomains() {
        try {
            List<String> list = service.getZooKeeper().getChildren(ZkPath.CONTAINER_DOMAINS.getPath(getId()));
            Collections.sort(list);
            return Collections.unmodifiableList(list);
        } catch (Exception e) {
            logger.warn("Error while retrieving jmx domains. This exception will be ignored.", e);
            return Collections.emptyList();
        }
    }

    public void start() {
        service.startContainer(this);
    }

    @Override
    public void stop() {
        service.stopContainer(this);
    }

    @Override
    public void destroy() {
        if (!hasAliveChildren()) {
            service.destroyContainer(this);
        } else {
            throw new IllegalStateException("Container " + id + " has one or more child containers alive and cannot be destroyed.");
        }
    }

    public Container[] getChildren() {
        List<Container> children = new ArrayList<Container>();
        for (Container container : service.getContainers()) {
            if (container.getParent() != null && getId().equals(container.getParent().getId())) {
                children.add(container);
            }
        }
        return children.toArray(new Container[0]);
    }

    public String getType() {
        return "karaf";
    }

    @Override
    public String getProvisionResult() {
        return getZkData(ZkPath.CONTAINER_PROVISION_RESULT);
    }

    @Override
    public String getProvisionException() {
        return getZkData(ZkPath.CONTAINER_PROVISION_EXCEPTION);
    }

    @Override
    public List<String> getProvisionList() {
        String str = getZkData(ZkPath.CONTAINER_PROVISION_LIST);
        return str != null ? Arrays.asList(str.split("\n")) : null;
    }

    @Override
    public CreateContainerMetadata<?> getMetadata() {
        try {
            if (metadata == null) {
                if (service.getZooKeeper().exists(ZkPath.CONTAINER_METADATA.getPath(id)) != null) {
                    //The metadata are stored encoded so that they are import/export friendly.
                    String encoded = service.getZooKeeper().getStringData(ZkPath.CONTAINER_METADATA.getPath(id));
                    byte[] decoded = Base64Encoder.decode(encoded).getBytes(Base64Encoder.base64CharSet);
                    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decoded));
                    metadata = (CreateContainerMetadata) ois.readObject();

                }
            }
            return metadata;
        } catch (Exception e) {
            logger.warn("Error while retrieving metadata. This exception will be ignored.", e);
            return null;
        }
    }

    public void setMetadata(CreateContainerMetadata<?> metadata) {
        this.metadata = metadata;
        try {
            //We need to check if zookeeper is available.
            if (service.getZooKeeper().isConnected()) {
                byte[] metadataBytes = ObjectUtils.toBytes(metadata);
                byte[] encoded = Base64Encoder.encode(metadataBytes);
                ZooKeeperUtils.set(service.getZooKeeper(), ZkPath.CONTAINER_METADATA.getPath(id), new String(encoded));
            }
        } catch (Exception e) {
            logger.warn("Error while storing metadata. This exception will be ignored.", e);
        }
    }

    /**
     * Checks if container requires upgrade/rollback operation.
     *
     * @param version
     * @return
     */
    private boolean requiresUpgrade(Version version) {
        Boolean requiresUpgrade = false;
        Profile[] oldProfiles = getProfiles();

        if (version.compareTo(getVersion()) == 0) {
            return false;
        }

        for (int i = 0; i < oldProfiles.length; i++) {
            // get new profile
            Profile newProfile = version.getProfile(oldProfiles[i].getId());
            if (!oldProfiles[i].configurationEquals(newProfile)) {
                requiresUpgrade = true;
            }
        }
        return requiresUpgrade;
    }

    /**
     * Checks if Container is root and has alive children.
     *
     * @return
     */
    public boolean hasAliveChildren() {
        for (Container child : getChildren()) {
            if (child.isAlive()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContainerImpl container = (ContainerImpl) o;

        if (id != null ? !id.equals(container.id) : container.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Container[" +
                "id=" + id +
                (parent != null ? ", parent=" + parent.getId() : "") +
                ']';
    }

    public boolean isAliveAndOK() {
        String status = getProvisionStatus();
        return isAlive() && (status == null || status.length() == 0 || status.toLowerCase().startsWith("success"));
    }
}
