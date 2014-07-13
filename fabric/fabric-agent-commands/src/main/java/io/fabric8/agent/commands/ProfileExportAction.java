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
package io.fabric8.agent.commands;

import java.io.File;

import io.fabric8.api.FabricService;
import io.fabric8.api.Version;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = "profile-export", scope = "fabric", description = ProfileExport.DESCRIPTION)
public class ProfileExportAction extends AbstractAction {

    @Option(name = "--version", description = "The profile version to export profiles from. Defaults to the current version if none specified.")
    private String version;

    @Argument(index = 0, required = true, name = "outputZipFileName", description = "The output file name of the generated ZIP of the profiles")
    private File outputZipFileName;

    @Argument(index = 1, required = false, name = "wildcard", description = "The file wildcard used to match the profile folders. e.g. 'examples/*'. If no wildcard is specified all profiles will be exported")
    private String wildcard;

    private final FabricService fabricService;

    ProfileExportAction(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    @Override
    protected Object doExecute() throws Exception {
        Version ver = version != null ? fabricService.getVersion(version) : fabricService.getDefaultVersion();
        if (ver == null) {
            if (version != null) {
                System.out.println("Version " + version + " does not exist!");
            } else {
                System.out.println("No default version available!");
            }
            return null;
        }

        fabricService.getDataStore().exportProfiles(ver.getId(), outputZipFileName.getAbsolutePath(), wildcard);
        System.out.println("Exported profiles to " + outputZipFileName.getCanonicalPath());
        return null;
    }
}
