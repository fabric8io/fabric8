/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.commands.support.FabricCommand;

@Command(name = "profile-delete", scope = "fabric", description = "Delete an existing profile")
public class ProfileDelete extends FabricCommand {

    @Option(name = "--version")
    private String version = "base";

    @Argument(index = 0, required = true, name = "profile")
    @CompleterValues(index = 0)
    private String name;

    @Override
    protected Object doExecute() throws Exception {
        Version version = fabricService.getVersion(this.version);

        for (Profile profile : version.getProfiles()) {
            if (name.equals(profile.getId())) {
                profile.delete();
            }
        }
        return null;
    }

}
