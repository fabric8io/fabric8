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
package org.fusesource.patch.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.fusesource.patch.Patch;
import org.fusesource.patch.PatchException;
import org.fusesource.patch.Service;

@Command(scope = "patch", name = "rollback", description = "Rollback a patch installation")
public class Rollback extends PatchCommandSupport {

    @Argument(name = "PATCH", description = "name of the patch to rollback", required = true, multiValued = false)
    String patchId;
    
    @Override
    protected void doExecute(Service service) throws Exception {
        Patch patch = service.getPatch(patchId);
        if (patch == null) {
            throw new PatchException("Patch '" + patchId + "' not found");
        }
        if (!patch.isInstalled()) {
            throw new PatchException("Patch '" + patchId + "' is not installed");
        }
        patch.rollback(false);
    }

}
