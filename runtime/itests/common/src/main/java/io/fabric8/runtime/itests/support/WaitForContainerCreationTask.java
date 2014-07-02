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
package io.fabric8.runtime.itests.support;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;

import java.util.concurrent.Callable;

import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceLocator;

/**
 * A {@link java.util.concurrent.Callable} that waits for the {@link io.fabric8.api.Container} to get created.
 */
public class WaitForContainerCreationTask implements Callable<Boolean> {

    private final FabricService fabricService;
    private final Long provisionTimeOut;
    private final String container;

    public WaitForContainerCreationTask(String container, Long provisionTimeOut) {
        ModuleContext moduleContext = RuntimeLocator.getRequiredRuntime().getModuleContext();
        this.fabricService = ServiceLocator.awaitService(moduleContext, FabricService.class);
        this.provisionTimeOut = provisionTimeOut;
        this.container = container;
    }

    @Override
    public Boolean call() throws Exception {
        for (long t = 0; (!containerExists() && t < provisionTimeOut); t += 2000L) {
            Thread.sleep(2000L);
            System.out.println("Container:" + container + " Exists:" + containerExists());
        }
        return containerExists();
    }

    private boolean containerExists() {
        try {
            Container c = fabricService.getContainer(container);
            return c != null;
        } catch (Exception ex) {
            return false;
        }
    }
}
