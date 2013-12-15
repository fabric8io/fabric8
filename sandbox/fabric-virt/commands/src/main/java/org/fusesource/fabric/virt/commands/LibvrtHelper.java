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

import java.util.LinkedHashSet;
import java.util.Set;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

public class LibvrtHelper {

    public static Set<Domain> getDomains(Connect connect, boolean active, boolean defined) {
        Set<Domain> domains = new LinkedHashSet<Domain>();

        if (active) {
            try {
                int[] activeDomainIds = connect.listDomains();
                for (int domianId : activeDomainIds) {
                    Domain activeDomain = connect.domainLookupByID(domianId);
                    domains.add(activeDomain);
                }
            } catch (LibvirtException e) {
                //Ignore
            }
        }

        if (defined) {
            try {
                String[] definedDomainNames = connect.listDefinedDomains();
                for (String definedDomainName : definedDomainNames) {
                    Domain definedDomain = connect.domainLookupByName(definedDomainName);
                    domains.add(definedDomain);
                }
            } catch (LibvirtException e) {
                //Ignore
            }
        }

        return domains;
    }

    private LibvrtHelper() {
        //Utility Class
    }
}
