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
package io.fabric8.support.karaf;

import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.support.api.Collector;
import io.fabric8.support.api.Resource;
import io.fabric8.support.api.ResourceFactory;
import org.apache.felix.scr.ScrService;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import java.util.LinkedList;
import java.util.List;

/**
 * {@link io.fabric8.support.api.Collector} that collects information about the Karaf container.
 */
@ThreadSafe
@Component(name = "io.fabric8.support.karaf.commands", label = "Fabric8 Support - Karaf Commands Collector", metatype = false)
@Service(Collector.class)
public class CommandCollector implements Collector {

    @Reference(referenceInterface = ScrService.class)
    private ScrService scrService;

    @Override
    public List<Resource> collect(ResourceFactory factory) {
        List<Resource> result = new LinkedList<>();
        result.add(factory.createCommandResource("osgi:headers --force org.jboss.fuse.esb-commands | grep Bundle-Version"));
          //result.add(new CommandResource("osgi:headers --force io.fabric8.common-util | grep Bundle-Version"));

        result.add(factory.createCommandResource("osgi:list -t 0"));
        result.add(factory.createCommandResource("osgi:ls"));
        result.add(factory.createCommandResource("osgi:headers"));
        result.add(factory.createCommandResource("log:display"));
        result.add(factory.createCommandResource("shell:info"));
        result.add(factory.createCommandResource("packages:imports"));
        result.add(factory.createCommandResource("packages:exports -i"));
        result.add(factory.createCommandResource("dev:classloaders"));
        result.add(factory.createCommandResource("dev:system-property"));
        result.add(factory.createCommandResource("scr:list"));
        result.add(factory.createCommandResource("jaas:realms"));
        result.add(factory.createCommandResource("config:list"));

        result.add(factory.createCommandResource("activemq:dstat"));
        result.add(factory.createCommandResource("activemq:bstat"));

        for (final org.apache.felix.scr.Component component : scrService.getComponents()) {
            result.add(factory.createCommandResource("scr:details " + component.getName()));
        }
        return result;
    }
}
