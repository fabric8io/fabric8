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
package io.fabric8.openshift.commands;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IOpenShiftConnection;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

@Command(name = "application-stop", scope = "openshift", description = "Stops the target application")
public class ApplicationStopAction extends OpenshiftCommandSupport {

    static final String FORMAT = "%-30s %s";

    @Option(name = "--domain", required = false, description = "Use only applications of that domain.")
    String domainId;

    @Argument(index = 0, name = "application", required = true, description = "The target application.")
    String applicationName;

    @Override
    protected Object doExecute() throws Exception {
        IOpenShiftConnection connection = getOrCreateConnection();
        for (IDomain domain : connection.getDomains()) {
            if (domainId == null || domainId.equals(domain.getId())) {
                IApplication application = domain.getApplicationByName(applicationName);
                application.stop();
            }
        }
        return null;
    }
}
