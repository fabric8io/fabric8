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
package io.fabric8.runtime.itests.karaf;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.runtime.itests.support.CommandSupport;
import io.fabric8.runtime.itests.support.FabricTestSupport;
import io.fabric8.runtime.itests.support.Provision;
import io.fabric8.runtime.itests.support.ServiceLocator;
import io.fabric8.runtime.itests.support.ServiceProxy;

import java.util.Arrays;

import org.apache.karaf.admin.AdminService;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("[FABRIC-819] Provide initial set of portable fabric smoke tests")
public class JoinTest {

    private static final String WAIT_FOR_JOIN_SERVICE = "wait-for-service io.fabric8.boot.commands.service.JoinAvailable";

	@After
	public void tearDown() throws InterruptedException {
	}

	@Test
	public void testJoin() throws Exception {
        System.err.println(CommandSupport.executeCommand("fabric:create -n"));
        ModuleContext moduleContext = RuntimeLocator.getRequiredRuntime().getModuleContext();;
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();

            AdminService adminService = ServiceLocator.awaitService(moduleContext, AdminService.class);
            String version = System.getProperty("fabric.version");
            System.err.println(CommandSupport.executeCommand("admin:create --featureURL mvn:io.fabric8/fabric8-karaf/" + version + "/xml/features --feature fabric-git --feature fabric-agent --feature fabric-boot-commands child1"));
            try {
                System.err.println(CommandSupport.executeCommand("admin:start child1"));
                Provision.instanceStarted(Arrays.asList("child1"), FabricTestSupport.PROVISION_TIMEOUT);
                System.err.println(CommandSupport.executeCommand("admin:list"));
                String joinCommand = "fabric:join -f --zookeeper-password "+ fabricService.getZookeeperPassword() +" " + fabricService.getZookeeperUrl();
                String response = "";
                for (int i = 0; i < 10 && !response.contains("true"); i++) {
                    response = CommandSupport.executeCommand("ssh -l admin -P admin -p " + adminService.getInstance("child1").getSshPort() + " localhost " + WAIT_FOR_JOIN_SERVICE);
                    Thread.sleep(1000);
                }

                System.err.println(CommandSupport.executeCommand("ssh -l admin -P admin -p " + adminService.getInstance("child1").getSshPort() + " localhost " + joinCommand));
                Provision.containersExist(Arrays.asList("child1"), FabricTestSupport.PROVISION_TIMEOUT);
                Container child1 = fabricService.getContainer("child1");
                System.err.println(CommandSupport.executeCommand("fabric:container-list"));
                Provision.containersStatus(Arrays.asList(child1), "success", FabricTestSupport.PROVISION_TIMEOUT);
                System.err.println(CommandSupport.executeCommand("fabric:container-list"));
            } finally {
                System.err.println(CommandSupport.executeCommand("admin:stop child1"));
            }
        } finally {
            fabricProxy.close();
        }
	}
}
