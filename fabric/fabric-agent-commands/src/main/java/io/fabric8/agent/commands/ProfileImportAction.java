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

import io.fabric8.api.FabricService;
import io.fabric8.api.Version;
import io.fabric8.api.scr.support.Strings;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

import java.util.List;

@Command(name = "profile-import", scope = "fabric", description = ProfileImport.DESCRIPTION)
public class ProfileImportAction extends AbstractAction {

    @Option(name = "--version", description = "The profile version to import the profiles into. Defaults to the current default version if none specified.")
    private String version;

    @Option(name = "-n", aliases = "--new", description = "Forces a new version to be created if no version option is specified")
    private boolean newVersion;

    @Argument(index = 0, required = true, multiValued = true, name = "profileUrls", description = "The URLs for one or more profile ZIP files to install; usually of the form mvn:groupId/artifactId/version/zip/profile")
    @CompleterValues(index = 0)
    private List<String> profileUrls;

    private final FabricService fabricService;

    ProfileImportAction(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    @Override
    protected Object doExecute() throws Exception {
        if (Strings.isNullOrBlank(version)) {
            if (newVersion) {
                // TODO create a new version
            }
        }
        Version ver = version != null ? fabricService.getVersion(version) : fabricService.getDefaultVersion();
        if (ver == null) {
            if (version != null) {
                System.out.println("version " + version + " does not exist!");
            } else {
                System.out.println("No default version available!");
            }
            return null;
        }

        fabricService.getDataStore().importProfiles(ver.getId(), profileUrls);
        return null;
    }
}
