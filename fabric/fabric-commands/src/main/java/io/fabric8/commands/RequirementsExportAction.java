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

import io.fabric8.api.FabricRequirements;
import io.fabric8.api.FabricService;
import io.fabric8.internal.RequirementsJson;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

import java.io.File;
import java.io.FileOutputStream;

@Command(name = RequirementsExport.FUNCTION_VALUE, scope = "fabric", description = RequirementsExport.DESCRIPTION)
public class RequirementsExportAction extends AbstractAction {
    @Argument(index = 0, required = true, description = "Output file name")
    protected File outputFile;

    private final FabricService fabricService;

    RequirementsExportAction(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    @Override
    protected Object doExecute() throws Exception {
        outputFile.getParentFile().mkdirs();

        FabricRequirements requirements = getFabricService().getRequirements();
        RequirementsJson.writeRequirements(new FileOutputStream(outputFile), requirements);
        System.out.println("Exported the fabric requirements to " + outputFile.getCanonicalPath());
        return null;
    }
}
