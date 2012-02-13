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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.apache.zookeeper.KeeperException;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.api.data.BundleInfo;
import org.fusesource.fabric.api.data.ServiceInfo;
import org.fusesource.fabric.service.ContainerTemplate;
import org.fusesource.fabric.service.FabricServiceImpl;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.osgi.jmx.framework.BundleStateMBean;
import org.osgi.jmx.framework.ServiceStateMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContainerImpl implements Container {

    /**
     * Logger.
     */
    protected transient Logger logger = LoggerFactory.getLogger(getClass());

    private final Container parent;
    private final String id;
    private final FabricServiceImpl service;

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
    public boolean isProvisioningComplete() {
        // for some reason isRoot() means we don't seem to get a provision result / exception
        return getProvisionResult() != null || getProvisionException() != null || isRoot();
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
        return getZkData(ZkPath.CONTAINER_SSH);
    }

    public String getJmxUrl() {
        return getZkData(ZkPath.CONTAINER_JMX);
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
    public Version getVersion() {
        try {
            String version = service.getZooKeeper().getStringData(ZkPath.CONFIG_CONTAINER.getPath(id));
            return new VersionImpl(version, service);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setVersion(Version version) {
        try {
            Profile[] profiles = getProfiles();
            StringBuilder builder = new StringBuilder();
            if (profiles != null && profiles.length > 0) {
                builder.append(profiles[0].getId());
                for (int i=1;i<profiles.length;i++) {
                    builder.append(" ").append(profiles[i].getId());
                }
            } else {
                builder.append("default");
            }

            ZooKeeperUtils.set( service.getZooKeeper(), ZkPath.CONFIG_CONTAINER.getPath(id), version.getName() );
            ZooKeeperUtils.set( service.getZooKeeper(), ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version.getName(),id), builder.toString());
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
            for (String p : str.split(" ")) {
                profiles.add(new ProfileImpl(p, version, service));
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
            String str = "";
            for (Profile parent : profiles) {
                if (!version.equals(parent.getVersion())) {
                    throw new IllegalArgumentException("Version mismatch setting profile " + parent + " with version "
                            + parent.getVersion() + " expected version " + version);
                }
                if (!str.isEmpty()) {
                    str += " ";
                }
                str += parent.getId();
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
            ZooKeeperUtils.set( service.getZooKeeper(), path, location );
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    //
    // TODO: remove these deprecated methods in the next release.
    //
    @Deprecated
    private ContainerTemplate containerTemplate;
    @Deprecated
    public ContainerTemplate getContainerTemplate() {
        if( containerTemplate ==null ) {
            containerTemplate = new ContainerTemplate(this, false);
        }
        return containerTemplate;
    }
    @Deprecated
    public ServiceInfo[] getServices() {
        return getServices(getContainerTemplate());
    }
    @Deprecated
    public BundleInfo[] getBundles() {
        return getBundles(containerTemplate);
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
        service.destroy(this);
    }

    public Container[] getChildren() {
        return new Container[0];
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
}
