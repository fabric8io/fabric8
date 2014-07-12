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
import io.fabric8.api.ProfileRequirements;
import io.fabric8.commands.support.ChangeRequirementSupport;
import io.fabric8.internal.RequirementsJson;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

@Command(name = RequirementsImport.FUNCTION_VALUE, scope = "fabric", description = RequirementsImport.DESCRIPTION)
public class RequirementsImportAction extends AbstractAction {
    @Argument(index = 0, required = true, description = "Requirements JSON URL")
    protected String jsonUrl;

    private final FabricService fabricService;

    RequirementsImportAction(FabricService fabricService) {
            this.fabricService = fabricService;
        }

    public FabricService getFabricService() {
        return fabricService;
    }

    @Override
    protected Object doExecute() throws Exception {
        // lets test if the file exists
        File file = new File(jsonUrl);
        InputStream is;
        if (file.exists()) {
            if (file.isDirectory()) {
                System.out.println("The file " + jsonUrl + " is a directory!");
                return null;
            }
            is = new FileInputStream(jsonUrl);
        } else {
            is = new URL(jsonUrl).openStream();
        }
        if (is == null) {
            System.out.println("Could not open the URL " + jsonUrl);
            return null;
        }

        FabricRequirements requirements = RequirementsJson.readRequirements(is);
        if (requirements != null) {
            getFabricService().setRequirements(requirements);

        }
        return null;
    }
}
