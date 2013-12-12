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

package io.fabric8.virt.commands;

import java.util.Set;
import org.apache.felix.gogo.commands.Command;
import org.libvirt.Connect;
import org.libvirt.Domain;

@Command(scope = "virt", name = "domain-list")
public class ListDomains extends LibvirtCommandSupport {

    protected static final String OUTPUTFORMAT = "%-20s %-10s";

    @Override
    protected Object doExecute() throws Exception {

        Connect connect = getConnection();
        Set<Domain> domains = LibvrtHelper.getDomains(connect, true, true);

        if (domains != null && !domains.isEmpty()) {
            System.out.println(String.format(OUTPUTFORMAT, "[Name]", "[State]"));
            for (Domain domain : domains) {
                String name = domain.getName();
                String state = domain.getInfo().state.name();
                state = state.substring(state.lastIndexOf("_") + 1);
                System.out.println(String.format(OUTPUTFORMAT, name, state.toLowerCase()));
            }
        }
        return null;
    }
}
