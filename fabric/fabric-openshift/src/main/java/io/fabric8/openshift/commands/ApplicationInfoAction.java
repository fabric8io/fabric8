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

import java.util.List;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IGear;
import com.openshift.client.IGearGroup;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.cartridge.IEmbeddedCartridge;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

@Command(name = "application-list", scope = "openshift", description = "Lists available openshift application")
public class ApplicationInfoAction extends OpenshiftCommandSupport {

    static final String FORMAT = "%-30s %s";

    @Option(name = "--domain", required = false, description = "Show only applications of that domain.")
    String domainId;

    @Argument(index = 0, name = "application", required = true, description = "The target application.")
    String applicationName;

    @Override
    protected Object doExecute() throws Exception {
        IOpenShiftConnection connection = getOrCreateConnection();
        for (IDomain domain : connection.getDomains()) {
            if (domainId == null || domainId.equals(domain.getId())) {
                IApplication application = domain.getApplicationByName(applicationName);
                System.out.println(String.format(FORMAT, "Name:", application.getName()));
                System.out.println(String.format(FORMAT, "UUID:", application.getUUID()));
                System.out.println(String.format(FORMAT, "Application URL:", application.getApplicationUrl()));
                System.out.println(String.format(FORMAT, "Git URL:", application.getGitUrl()));
                System.out.println(String.format(FORMAT, "SSH URL:", application.getSshUrl()));
                System.out.println(String.format(FORMAT, "Cartridge:", application.getCartridge().getName()));
                System.out.println(String.format(FORMAT, "Embedded Cartridges:", cartridgesToString(application.getEmbeddedCartridges())));
                System.out.println(String.format(FORMAT, "Scale:", application.getApplicationScale().getValue()));
                System.out.println(String.format(FORMAT, "Gear Profile:", application.getGearProfile().getName()));

                // find the gear state
                String state = null;
                for (IGearGroup group : application.getGearGroups()) {
                    for (IGear gear : group.getGears()) {
                        state = gear.getState().name().toLowerCase();
                    }
                }
                if (state != null) {
                    System.out.println(String.format(FORMAT, "Gear State:", state));
                }
            }
        }
        return null;
    }

    private static String cartridgesToString(List<IEmbeddedCartridge> cartridges) {
        StringBuilder sb = new StringBuilder();
        for (IEmbeddedCartridge cartridge : cartridges) {
            sb.append(cartridge.getName()).append(" ");
        }
        return sb.toString();
    }
}
