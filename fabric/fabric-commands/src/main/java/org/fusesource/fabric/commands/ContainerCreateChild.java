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

import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.commands.support.ContainerCreateSupport;

@Command(name = "container-create-child", scope = "fabric", description = "Creates one or more child containers")
public class ContainerCreateChild extends ContainerCreateSupport {

    @Argument(index = 0, required = true, description = "Parent containers ID")
    protected String parent;
    @Argument(index = 1, required = true, description = "The name of the containers to be created. When creating multiple containers it serves as a prefix")
    protected String name;
    @Argument(index = 2, required = false, description = "The number of containers that should be created")
    protected int number = 1;

    @Override
    protected Object doExecute() throws Exception {
        // validate profiles exists before creating
        doValidateProfiles();

        // okay create child container
        String url = "child://" + parent;
        Container[] children = fabricService.createContainers(url, name, isEnsembleServer, debugContainer, number);
        // and set its profiles after creation
        setProfiles(children);
        return null;
    }
    
    protected void doValidateProfiles() {
        // get the profiles for the given version
        Version ver = version != null ? fabricService.getVersion(version) : fabricService.getDefaultVersion();
        Profile[] profiles = ver.getProfiles();

        // validate profiles exists before creating a new container
        List<String> names = getProfileNames();
        for (String profile : names) {
            if (!hasProfile(profiles, name)) {
                throw new IllegalArgumentException("Profile " + profile + " with version " + ver.getName() + " does not exist");
            }
        }
    }

    private static boolean hasProfile(Profile[] profiles, String name) {
        if (profiles == null || profiles.length == 0) {
            return false;
        }

        for (Profile profile : profiles) {
            if (profile.getId().equals(name)) {
                return true;
            }
        }

        return false;
    }

}
