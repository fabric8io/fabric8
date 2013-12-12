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
package io.fabric8.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import io.fabric8.boot.commands.support.FabricCommand;

@Command(name = "container-default-jmv-options", scope = "fabric", description = "Get or set the default JVM options to use when creating a new container")
public class ContainerDefaultJvmOptions extends FabricCommand {

    @Argument(index = 0, name = "jvmOptions", description = "The default JVM options to use, or empty to show the default", required = false, multiValued = false)
    private String options;
    
    protected Object doExecute() throws Exception {
        checkFabricAvailable();

        if (options != null) {
            fabricService.setDefaultJvmOptions(options);
        } else {
            System.out.println(fabricService.getDefaultJvmOptions());
        }
        return null;
    }

}
