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

import com.openshift.client.*;
import com.openshift.client.cartridge.StandaloneCartridge;
import com.openshift.internal.client.GearProfile;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

@Command(name = "application-create", scope = "openshift", description = "Creates an application")
public class ApplicationCreateAction extends OpenshiftCommandSupport {

    @Option(name = "--domain", required = false, description = "Create applications on that domain.")
    String domainId;

    @Option(name = "--gear-profile", description = "Gear profile controls how much memory and CPU your cartridges can use.")
    private String gearProfile = "small";

    @Option(name = "--scaling", description = "Enable scaling for the web cartridge.")
    private boolean scaling = false;

    @Argument(index = 0, name = "application", required = true, description = "The target application.")
    String applicationName;

    @Argument(index = 1, name = "cartridge", required = true, multiValued = false, description = "The cartridge to use.")
    String cartridge;

    @Override
    protected Object doExecute() throws Exception {
        IOpenShiftConnection connection = getOrCreateConnection();
        IUser user = connection.getUser();
        IDomain domain = domainId != null ? user.getDomain(domainId) : user.getDefaultDomain();
        if (domainId != null && domain == null) {
            domain = user.createDomain(domainId);
        }

        ApplicationScale scale = ApplicationScale.NO_SCALE;
        if( scaling ) {
            scale = ApplicationScale.SCALE;
        }
        IApplication application = domain.createApplication(applicationName, new StandaloneCartridge(cartridge), scale, new GearProfile(gearProfile));
        System.out.println(application.getCreationLog());
        return null;
    }
}
