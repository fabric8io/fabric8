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
import io.fabric8.api.EnsembleModificationFailed;
import io.fabric8.api.FabricService;

import java.util.Arrays;

public class FabricEnsembleSupport {

    public static final Long PROVISION_TIMEOUT = 60000L;

    public static void addToEnsemble(FabricService fabricService, Container... containers) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("fabric:ensemble-add --force --migration-timeout 240000 ");
        for (Container c : containers) {
            sb.append(c.getId()).append(" ");
        }

        doWithEnsemble(fabricService, sb.toString());
    }

    public static void removeFromEnsemble(FabricService fabricService, Container... containers) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("fabric:ensemble-remove --force --migration-timeout 240000 ");
        for (Container c : containers) {
            sb.append(c.getId()).append(" ");
        }

        doWithEnsemble(fabricService, sb.toString());
    }

    private static void doWithEnsemble(FabricService fabricService, String command) throws Exception {
        long start = System.currentTimeMillis();
        long now = System.currentTimeMillis();
        boolean keepRunning = true;

        while (!Thread.currentThread().isInterrupted() && keepRunning && now - start <= 30000L) {
            try {
                System.err.println(CommandSupport.executeCommand(command));
                keepRunning = false;
            } catch (Exception ex) {
                if (isRetriable(ex)) {
                    System.err.println("Not ready for ensemble modification! Retrying...");
                    Provision.provisioningSuccess(Arrays.asList(fabricService.getContainers()), PROVISION_TIMEOUT);
                    now = System.currentTimeMillis();
                } else {
                    throw ex;
                }
            }
        }
    }

    private static boolean isRetriable(Throwable th) {
        if (th instanceof CommandExecutionException) {
            return isRetriable(th.getCause());
        } if (th instanceof EnsembleModificationFailed) {
            return ((EnsembleModificationFailed) th).getReason() == EnsembleModificationFailed.Reason.CONTAINERS_NOT_ALIVE;
        } else {
            return false;
        }
    }
}