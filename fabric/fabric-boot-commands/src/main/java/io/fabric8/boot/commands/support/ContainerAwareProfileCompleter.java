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
package io.fabric8.boot.commands.support;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import io.fabric8.api.RuntimeProperties;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.ArgumentCompleter;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.apache.karaf.shell.console.jline.CommandSessionHolder;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.utils.SystemProperties;

/**
 * A completer that is aware of the target container.
 * The target container is being looked up from the command arguments.
 * If no container is found the current container is assumed.
 */
public class ContainerAwareProfileCompleter implements Completer {

    private final int containerArgumentIndex;
    private final boolean assigned;
    private final boolean unassigned;

    protected FabricService fabricService;
    protected RuntimeProperties runtimeProperties;

    public ContainerAwareProfileCompleter(int containerArgumentIndex, boolean assigned, boolean unassigned) {
        this.containerArgumentIndex = containerArgumentIndex;
        this.assigned = assigned;
        this.unassigned = unassigned;
    }

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        try {
            Version version = fabricService.getDefaultVersion();

            Container container = fabricService.getCurrentContainer();

            try{
                container =  fabricService.getContainer(getContainer(CommandSessionHolder.getSession(), containerArgumentIndex));
            } catch (Exception ex) {
                //Igonre and use current container.
            }

            Profile[] containerProfiles = container.getProfiles();
            List<String> containerProfileNames = new LinkedList<String>();
            if (containerProfiles != null) {
                for (Profile p : containerProfiles) {
                    containerProfileNames.add(p.getId());
                }
            }

            Profile[] profiles = version.getProfiles();
            List<String> allProfileNames = new LinkedList<String>();
            if (containerProfiles != null) {
                for (Profile p : profiles) {
                    allProfileNames.add(p.getId());
                }
            }

            if ( assigned && unassigned) {
                delegate.getStrings().addAll(allProfileNames);
            } else if (assigned) {
                delegate.getStrings().addAll(containerProfileNames);
            } else if (unassigned) {
                allProfileNames.removeAll(containerProfileNames);
                delegate.getStrings().addAll(allProfileNames);
            }
        } catch (Exception ex) {
            //Ignore Exceptions
        }
        return delegate.complete(buffer, cursor, candidates);
    }


    /**
     * Retrieves the container name from the arugment list on the specified index.
     * @param commandSession
     * @return
     */
    private String getContainer(CommandSession commandSession, int index) {
        String containerName = runtimeProperties.getProperty(SystemProperties.KARAF_NAME);
        ArgumentCompleter.ArgumentList list = (ArgumentCompleter.ArgumentList) commandSession.get(ArgumentCompleter.ARGUMENTS_LIST);
        if (list != null && list.getArguments() != null && list.getArguments().length > 0) {
            List<String> arguments = Arrays.asList(list.getArguments());
            if (arguments.size() > index) {
                containerName = arguments.get(index);
            }
        }
        return containerName;
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public RuntimeProperties getRuntimeProperties() {
        return runtimeProperties;
    }

    public void setRuntimeProperties(RuntimeProperties runtimeProperties) {
        this.runtimeProperties = runtimeProperties;
    }
}
