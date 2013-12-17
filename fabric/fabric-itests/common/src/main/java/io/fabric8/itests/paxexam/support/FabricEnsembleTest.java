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
import org.fusesource.tooling.testing.pax.exam.karaf.CommandExecutionException;

import java.util.Arrays;

public class FabricEnsembleTest extends FabricTestSupport {


   public void addToEnsemble(Container... containers) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("fabric:ensemble-add --force --migration-timeout 240000 ");
        for (Container c : containers) {
            sb.append(c.getId()).append(" ");
        }

        try {
            System.err.println(executeCommand(sb.toString(), 240000L, false));
        } catch (CommandExecutionException e) {
            if (isRetriable(e)) {
                System.err.println("Retrying...");
                Provision.provisioningSuccess(Arrays.asList(getFabricService().getContainers()), PROVISION_TIMEOUT);
                System.err.println(executeCommand(sb.toString(), 240000L, false));
            } else {
                throw e;
            }
        }
    }

   public void removeFromEnsemble(Container... containers) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("fabric:ensemble-remove --force --migration-timeout 240000 ");
        for (Container c : containers) {
            sb.append(c.getId()).append(" ");
        }

        try {
            System.err.println(executeCommand(sb.toString(), 240000L, false));
        } catch (CommandExecutionException e) {
            if (isRetriable(e)) {
                System.err.println("Retrying...");
                Provision.provisioningSuccess(Arrays.asList(getFabricService().getContainers()), PROVISION_TIMEOUT);
                System.err.println(executeCommand(sb.toString(), 240000L, false));
            } else {
                throw e;
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