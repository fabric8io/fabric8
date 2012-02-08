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

import java.net.URI;
import org.apache.karaf.admin.management.AdminServiceMBean;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.ContainerProvider;
import org.fusesource.fabric.api.CreateContainerArguments;
import org.fusesource.fabric.internal.FabricConstants;

public class ChildContainerProvider implements ContainerProvider {

    final FabricServiceImpl service;

    public ChildContainerProvider(FabricServiceImpl service) {
        this.service = service;
    }

    @Override
    public void create(URI proxyUri, final URI containerUri, final String name, final String zooKeeperUrl, final boolean isEnsembleServer, final boolean debugContainer, final int number) {
        String parentName = FabricServiceImpl.getParentFromURI(containerUri);
        final Container parent = service.getContainer(parentName);
        ContainerTemplate containerTemplate = service.getContainerTemplate(parent);

        //Retrieve the credentials from the URI if available.
        String ui = containerUri.getUserInfo();
        String[] uip = ui != null ? ui.split(":") : null;
        if (uip != null) {
            containerTemplate.setLogin(uip[0]);
            containerTemplate.setPassword(uip[1]);
        }

        containerTemplate.execute(new ContainerTemplate.AdminServiceCallback<Object>() {
            public Object doWithAdminService(AdminServiceMBean adminService) throws Exception {
                String javaOpts = zooKeeperUrl != null ? "-Dzookeeper.url=\"" + zooKeeperUrl + "\" -Xmx512M -server" : "";
                if (debugContainer) {
                    javaOpts += DEBUG_CONTAINER;
                }
                if (isEnsembleServer) {
                    javaOpts += ENSEMBLE_SERVER_CONTAINER;
                }
                String features = "fabric-agent";
                String featuresUrls = "mvn:org.fusesource.fabric/fuse-fabric/" + FabricConstants.VERSION + "/xml/features";

                for (int i = 1; i <= number; i++) {
                    String containerName = name;
                    if (number > 1) {
                        containerName += i;
                    }
                    adminService.createInstance(containerName, 0, 0, 0, null, javaOpts, features, featuresUrls);
                    adminService.startInstance(containerName, null);
                }
                return null;
            }
        });
    }

    @Override
    public void create(URI proxyUri, URI containerUri, String name, String zooKeeperUrl, boolean isEnsembleServer, boolean debugContainer) {
        create(proxyUri, containerUri, name, zooKeeperUrl, isEnsembleServer, debugContainer,1);
    }

    @Override
    public void create(URI proxyUri, URI containerUri, String name, String zooKeeperUrl) {
        create(proxyUri, containerUri, name, zooKeeperUrl,false, false);
    }

    @Override
    public boolean create(CreateContainerArguments args, String name, String zooKeeperUrl) throws Exception {
        return false;
    }
}
