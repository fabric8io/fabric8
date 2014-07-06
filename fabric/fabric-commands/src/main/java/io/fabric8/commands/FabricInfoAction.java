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
import io.fabric8.api.RuntimeProperties;
import io.fabric8.utils.FabricVersionUtils;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

import static io.fabric8.common.util.Strings.emptyIfNull;

@Command(name = FabricInfo.FUNCTION_VALUE, scope = FabricInfo.SCOPE_VALUE, description = FabricInfo.DESCRIPTION)
public class FabricInfoAction extends AbstractAction {

	static final String FORMAT = "%-30s %s";

    private final FabricService fabricService;
    private final RuntimeProperties runtimeProperties;

    FabricInfoAction(FabricService fabricService, RuntimeProperties runtimeProperties) {
        this.fabricService = fabricService;
        this.runtimeProperties = runtimeProperties;
    }

	@Override
	protected Object doExecute() throws Exception {
        System.out.println(String.format(FORMAT, "Fabric Version:", emptyIfNull(FabricVersionUtils.getReleaseVersion())));
        System.out.println(String.format(FORMAT, "Web Console:", emptyIfNull(fabricService.getWebConsoleUrl())));
        System.out.println(String.format(FORMAT, "Rest API:", emptyIfNull(fabricService.getRestAPI())));
        System.out.println(String.format(FORMAT, "Git URL:", emptyIfNull(fabricService.getGitUrl())));
        System.out.println(String.format(FORMAT, "ZooKeeper URI:", emptyIfNull(fabricService.getZookeeperUrl())));
		System.out.println(String.format(FORMAT, "Maven Download URI:", fabricService.getMavenRepoURI()));
		System.out.println(String.format(FORMAT, "Maven Upload URI:", fabricService.getMavenRepoUploadURI()));

		return null;
	}

}
