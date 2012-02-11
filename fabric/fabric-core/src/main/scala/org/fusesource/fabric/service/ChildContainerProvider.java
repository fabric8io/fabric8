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

import org.apache.karaf.admin.management.AdminServiceMBean;
import org.fusesource.fabric.api.*;
import org.fusesource.fabric.internal.FabricConstants;

import java.util.LinkedHashSet;
import java.util.Set;

public class ChildContainerProvider implements ContainerProvider<CreateContainerChildOptions, CreateContainerChildMetadata> {

    final FabricServiceImpl service;

    public ChildContainerProvider(FabricServiceImpl service) {
        this.service = service;
    }

    @Override
    public Set<CreateContainerChildMetadata> create(final CreateContainerChildOptions options) throws Exception {
        Set<CreateContainerChildMetadata> result = new LinkedHashSet<CreateContainerChildMetadata>();
        String parentName = options.getParent();
        final Container parent = service.getContainer(parentName);
        ContainerTemplate containerTemplate = service.getContainerTemplate(parent);

        //Retrieve the credentials from the URI if available.
        String ui = options.getProviderURI().getUserInfo();
        String[] uip = ui != null ? ui.split(":") : null;
        if (uip != null) {
            containerTemplate.setLogin(uip[0]);
            containerTemplate.setPassword(uip[1]);
        }

        containerTemplate.execute(new ContainerTemplate.AdminServiceCallback<Object>() {
            public Object doWithAdminService(AdminServiceMBean adminService) throws Exception {
                String javaOpts =  options.getZookeeperUrl() != null ? "-Dzookeeper.url=\"" + options.getZookeeperUrl() + "\" -Xmx512M -server" : "";
                if (options.isDebugContainer()) {
                    javaOpts += DEBUG_CONTAINER;
                }
                if (options.isEnsembleServer()) {
                    javaOpts += ENSEMBLE_SERVER_CONTAINER;
                }
                String features = "fabric-agent";
                String featuresUrls = "mvn:org.fusesource.fabric/fuse-fabric/" + FabricConstants.FABRIC_VERSION + "/xml/features";

                for (int i = 1; i <= options.getNumber(); i++) {
                    String containerName = options.getName();
                    if (options.getNumber() > 1) {
                        containerName += i;
                    }
                    adminService.createInstance(containerName, 0, 0, 0, null, javaOpts, features, featuresUrls);
                    adminService.startInstance(containerName, null);

                    CreateContainerChildMetadata metadata = new CreateContainerChildMetadata();
                    metadata.setContainerName(containerName);
                }
                return null;
            }
        });
        return result;
    }
}
