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
package org.fusesource.fabric.service;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.karaf.admin.management.AdminServiceMBean;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.ContainerProvider;
import org.fusesource.fabric.api.CreateContainerChildMetadata;
import org.fusesource.fabric.api.CreateContainerChildOptions;
import org.fusesource.fabric.internal.FabricConstants;

public class ChildContainerProvider implements ContainerProvider<CreateContainerChildOptions, CreateContainerChildMetadata> {

    final FabricServiceImpl service;

    public ChildContainerProvider(FabricServiceImpl service) {
        this.service = service;
    }

    @Override
    public Set<CreateContainerChildMetadata> create(final CreateContainerChildOptions options) throws Exception {
        final Set<CreateContainerChildMetadata> result = new LinkedHashSet<CreateContainerChildMetadata>();
        String parentName = options.getParent();
        final Container parent = service.getContainer(parentName);
        ContainerTemplate containerTemplate = service.getContainerTemplate(parent);

        //Retrieve the credentials from the URI if available.
        if (options.getProviderURI() != null && options.getProviderURI().getUserInfo() != null) {
            String ui = options.getProviderURI().getUserInfo();
            String[] uip = ui != null ? ui.split(":") : null;
            if (uip != null) {
                containerTemplate.setLogin(uip[0]);
                containerTemplate.setPassword(uip[1]);
            }
        }

        containerTemplate.execute(new ContainerTemplate.AdminServiceCallback<Object>() {
            public Object doWithAdminService(AdminServiceMBean adminService) throws Exception {
                StringBuilder jvmOptsBuilder = new StringBuilder();

                jvmOptsBuilder.append("-server -Dcom.sun.management.jmxremote ")
                        .append(options.getJvmOpts()).append(" ")
                        .append(options.getZookeeperUrl() != null ? "-Dzookeeper.url=\"" + options.getZookeeperUrl() + "\"" : "");

                if (!options.getJvmOpts().contains("-Xmx")) {
                    jvmOptsBuilder.append("-Xmx512m");
                }

                if (options.isDebugContainer()) {
                    jvmOptsBuilder.append(" ").append(DEBUG_CONTAINER);
                }
                if (options.isEnsembleServer()) {
                    jvmOptsBuilder.append(" ").append(ENSEMBLE_SERVER_CONTAINER);
                }
                String features = "fabric-agent";
                String featuresUrls = "mvn:org.fusesource.fabric/fuse-fabric/" + FabricConstants.FABRIC_VERSION + "/xml/features";

                for (int i = 1; i <= options.getNumber(); i++) {
                    String containerName = options.getName();
                    if (options.getNumber() > 1) {
                        containerName += i;
                    }
                    CreateContainerChildMetadata metadata = new CreateContainerChildMetadata();
                    metadata.setCreateOptions(options);
                    metadata.setContainerName(containerName);
                    try {
                        adminService.createInstance(containerName, 0, 0, 0, null, jvmOptsBuilder.toString(), features, featuresUrls);
                        adminService.startInstance(containerName, null);
                    } catch (Throwable t) {
                        metadata.setFailure(t);
                    }
                    result.add(metadata);
                }
                return null;
            }
        });
        return result;
    }

    @Override
    public void start(final Container container) {
        getContainerTemplate(container.getParent()).execute(new ContainerTemplate.AdminServiceCallback<Object>() {
            public Object doWithAdminService(AdminServiceMBean adminService) throws Exception {
                adminService.startInstance(container.getId(), null);
                return null;
            }
        });
    }

    @Override
    public void stop(final Container container) {
        getContainerTemplate(container.getParent()).execute(new ContainerTemplate.AdminServiceCallback<Object>() {
            public Object doWithAdminService(AdminServiceMBean adminService) throws Exception {
                adminService.stopInstance(container.getId());
                return null;
            }
        });
    }

    @Override
    public void destroy(final Container container) {
        getContainerTemplate(container.getParent()).execute(new ContainerTemplate.AdminServiceCallback<Object>() {
            public Object doWithAdminService(AdminServiceMBean adminService) throws Exception {
                try {
                    if (container.isAlive()) {
                        adminService.stopInstance(container.getId());
                    }
                } catch (Exception e) {
                    // Ignore if the container is stopped
                    if (container.isAlive()) {
                        throw e;
                    }
                }
                adminService.destroyInstance(container.getId());
                return null;
            }
        });
    }

    protected ContainerTemplate getContainerTemplate(Container container) {
        return new ContainerTemplate(container, false, service.getUserName(), service.getPassword());
    }
}
