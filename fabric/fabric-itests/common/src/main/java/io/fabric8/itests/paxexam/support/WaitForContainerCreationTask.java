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
package io.fabric8.itests.paxexam.support;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceLocator;

import java.util.concurrent.Callable;

import org.osgi.framework.BundleContext;

/**
 * A {@link java.util.concurrent.Callable} that waits for the {@link io.fabric8.api.Container} to get created.
 */
public class WaitForContainerCreationTask implements Callable<Boolean> {

    private final FabricService fabricService;
    private final Long provisionTimeOut;
    private final String container;

    public WaitForContainerCreationTask(BundleContext bundleContext, String container, Long provisionTimeOut) {
        this.fabricService = ServiceLocator.awaitService(bundleContext, FabricService.class);
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
