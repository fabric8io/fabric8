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

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.commands.support.FabricCommand;

/**
 *
 */
@Command(name = "profile-edit", scope = "fabric", description = "Edit a profile")
public class ProfileEdit extends FabricCommand {

    @Option(name = "-v", aliases = { "--version"}, description = "The version of the profile to edit")
    private String version = "base";

    @Option(name = "-p", aliases = "--profile", description = "The target profile to edit")
    private String target = "default";

    @Argument(index = 0, multiValued = true)
    private String arguments[];

    @Option(name = "--pid", description = "Target PID to edit")
    private String pid = null;

    @Option(name = "--repositories", description = "Edit repositories")
    private boolean repositories = false;

    @Option(name = "--features", description = "Edit features")
    private boolean features = false;

    @Option(name = "--bundles", description = "Edit bundles")
    private boolean bundles = false;

    @Option(name = "--set", description = "Set or create value(s)")
    private boolean set = true;

    @Option(name = "--delete", description = "Delete value(s)")
    private boolean delete = false;

    @Override
    protected Object doExecute() throws Exception {
        if (delete) {
            set = false;
        }
        Version version = fabricService.getVersion(this.version);

        for (Profile profile : version.getProfiles()) {
            if (target.equals(profile.getId())) {
                editProfile(profile);
            }
        }
        return null;
    }

    private void editProfile(Profile profile) throws Exception {
        String pid = getPid();

        Map<String, Map<String, String>> config = profile.getConfigurations();
        Map<String, String> pidConfig = config.get(pid);
        if (pidConfig == null) {
            pidConfig = new HashMap<String, String>();
        }

        String prefix = "";
        if (repositories) {
            prefix = "repository.";
        } else if (features) {
            prefix = "feature.";
        } else if (bundles) {
            prefix = "bundle.";
        }

        if (arguments != null) {
            for (String arg : arguments) {
                if (set) {
                    String[] nameValue = arg.split("=",2);
                    if (nameValue.length != 2) {
                        if (repositories || features || bundles) {
                            nameValue = new String[]{nameValue[0].replace('/', '_'), nameValue[0]};
                        } else {
                            throw new IllegalArgumentException(String.format("Argument \"%s\" is invalid, arguments need to be in the form of \"name=value\""));
                        }
                    }
                    pidConfig.put(prefix + nameValue[0], nameValue[1]);
                } else if (delete) {
                    if (repositories || features || bundles) {
                        for (Map.Entry<String, String> entry : new HashMap<String,String>(pidConfig).entrySet()) {
                            if(arg.equals(entry.getValue())) {
                                pidConfig.remove(entry.getKey());
                            }
                        }
                    } else {
                        pidConfig.remove(prefix + arg);
                    }
                }
            }
        }

        config.put(pid, pidConfig);
        profile.setConfigurations(config);
    }

    private String getPid() {
        if (pid != null) {
            return pid;
        } else {
            return AGENT_PID;
        }
    }
}
