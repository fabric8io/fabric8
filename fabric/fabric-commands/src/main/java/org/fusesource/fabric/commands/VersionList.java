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
package org.fusesource.fabric.commands;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.felix.gogo.commands.Command;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.commands.support.FabricCommand;

@Command(name = "version-list", scope = "fabric", description = "List existing versions")
public class VersionList extends FabricCommand {

    @Override
    protected Object doExecute() throws Exception {
        Version[] versions = fabricService.getVersions();
        printVersions(versions, fabricService.getDefaultVersion(), System.out);
        return null;
    }

    protected void printVersions(Version[] versions, Version defaultVersion, PrintStream out) {
        out.println(String.format("%-30s %-9s", "[version]", "[default]"));

        // we want to sort the versions
        List<Version> list = new ArrayList<Version>(versions.length);
        for (Version version : versions) {
            list.add(version);
        }
        Collections.sort(list, new VersionComparator());

        for (Version version : list) {
            boolean isDefault = defaultVersion.getName().equals(version.getName());
            out.println(String.format("%-30s %-9s", version.getName(), (isDefault ? "true" : "false")));
        }
    }
    
    private static final class VersionComparator implements Comparator<Version> {
        
        @Override
        public int compare(Version oldVersion, Version newVersion) {
            return oldVersion.getName().compareToIgnoreCase(newVersion.getName());
        }
    } 

}
