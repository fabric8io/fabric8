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
package io.fabric8.patch.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import io.fabric8.patch.Patch;
import io.fabric8.patch.PatchException;
import io.fabric8.patch.Result;
import io.fabric8.patch.Service;

@Command(scope = "patch", name = "simulate", description = "Simulate a patch installation")
public class SimulateAction extends PatchActionSupport {

    @Argument(name = "PATCH", description = "name of the patch to simulate", required = true, multiValued = false)
    String patchId;

    SimulateAction(Service service) {
        super(service);
    }

    @Override
    protected void doExecute(Service service) throws Exception {
        Patch patch = service.getPatch(patchId);
        if (patch == null) {
            throw new PatchException("Patch '" + patchId + "' not found");
        }
        if (patch.isInstalled()) {
            throw new PatchException("Patch '" + patchId + "' is already installed");
        }
        Result result = patch.simulate();
        display(result);
    }

}
