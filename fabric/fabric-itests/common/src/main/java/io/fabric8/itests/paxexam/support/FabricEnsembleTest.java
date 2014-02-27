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
import io.fabric8.api.EnsembleModificationFailed;
import io.fabric8.api.FabricService;

import java.util.Arrays;

import org.fusesource.tooling.testing.pax.exam.karaf.CommandExecutionException;

public class FabricEnsembleTest extends FabricTestSupport {


   public void addToEnsemble(FabricService fabricService, Container... containers) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("fabric:ensemble-add --force --migration-timeout 240000 ");
        for (Container c : containers) {
            sb.append(c.getId()).append(" ");
        }

       doWithEnsemble(fabricService, sb.toString());
    }

   public void removeFromEnsemble(FabricService fabricService, Container... containers) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("fabric:ensemble-remove --force --migration-timeout 240000 ");
        for (Container c : containers) {
            sb.append(c.getId()).append(" ");
        }

        doWithEnsemble(fabricService, sb.toString());
    }

    private void doWithEnsemble(FabricService fabricService, String command) throws Exception {
        long start = System.currentTimeMillis();
        long now = System.currentTimeMillis();
        boolean keepRunning = true;

        while (!Thread.currentThread().isInterrupted() && keepRunning &&  now - start <= 30000L) {
            try {
                System.err.println(executeCommand(command, 240000L - (start - now), false));
                keepRunning = false;
            } catch (CommandExecutionException e) {
                if (isRetriable(e)) {
                    System.err.println("Not ready for ensemble modification! Retrying...");
                    Provision.provisioningSuccess(Arrays.asList(fabricService.getContainers()), PROVISION_TIMEOUT, ContainerCallback.DISPLAY_ALL);
                    now = System.currentTimeMillis();
                } else {
                    throw e;
                }
            }
        }
    }

    private static boolean isRetriable(Throwable t) {
        if (t instanceof CommandExecutionException) {
            return isRetriable(t.getCause());
        } else if (t instanceof EnsembleModificationFailed) {
            return ((EnsembleModificationFailed) t).getReason() == EnsembleModificationFailed.Reason.CONTAINERS_NOT_ALIVE;
        } else {
            return false;
        }
    }
}