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
package io.fabric8.commands;

import io.fabric8.api.FabricService;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = ContainerDefaultJvmOptions.FUNCTION_VALUE, scope = ContainerDefaultJvmOptions.SCOPE_VALUE, description = ContainerDefaultJvmOptions.DESCRIPTION)
public class ContainerDefaultJvmOptionsAction extends AbstractAction {

    @Argument(index = 0, name = "jvmOptions", description = "The default JVM options to use, or empty to show the default", required = false, multiValued = false)
    private String options;

    private final FabricService fabricService;

    ContainerDefaultJvmOptionsAction(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    protected Object doExecute() throws Exception {
        if (options != null) {
            fabricService.setDefaultJvmOptions(options);
        } else {
            System.out.println(fabricService.getDefaultJvmOptions());
        }
        return null;
    }

}
