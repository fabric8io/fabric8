/*
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
package io.fabric8.commands;

import io.fabric8.api.Container;
import io.fabric8.api.FabricException;
import io.fabric8.boot.commands.support.FabricCommand;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

@Command(name = "wait-for-provisioning", scope = "fabric", description = "Waits for containers to be provisioned")
public class WaitForProvisioning extends FabricCommand {
    
    @Option(name = "-v", aliases = "--verbose", description = "Flag for verbose output", multiValued = false, required = false)
    private boolean verbose;
    
    @Option(name = "--provision-timeout", multiValued = false, description = "How long to wait (milliseconds) for the containers to provision")
    private long provisionTimeout = 120000L;
    
    @Override
    protected Object doExecute() throws Exception {
        
    	checkFabricAvailable();
        
        return waitForSuccessfulDeploymentOf();
        
    }

    private String waitForSuccessfulDeploymentOf() throws InterruptedException {
        
        long startedAt = System.currentTimeMillis();

        while (!Thread.interrupted() && startedAt + provisionTimeout > System.currentTimeMillis()) {
            try {
                Container[] fabric = fabricService.getContainers();
               
                if (isFabricProvisioned(fabric)){
                	return "SUCCESS";
               	}
                
                Thread.sleep(1000);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Throwable t) {
                throw FabricException.launderThrowable(t);
            }
        }
        return "ERROR";
        
    }

	private boolean isFabricProvisioned(Container[] fabric)
			throws InterruptedException {
		
		if (fabric == null){
			return false;
		}
		
		for(Container container : fabric){
			
			if (container == null || !container.isAlive() || !Container.PROVISION_SUCCESS.equals(container.getProvisionStatus())) {
				if (verbose){
					System.out.println(String.format("Waiting: Container %s is %s", container.getId(), container.getProvisionStatus()));	
				}
				if (container.getProvisionStatus() != null && container.getProvisionStatus().startsWith(Container.PROVISION_ERROR)){

					throw new FabricException("Error provisioning container " + container.getId() + " : " + container.getProvisionStatus());
				}
				
				return false;
			}
		}
		return true;
	}
}
