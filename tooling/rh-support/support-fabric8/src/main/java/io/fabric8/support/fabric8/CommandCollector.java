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
package io.fabric8.support.fabric8;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.support.api.Collector;
import io.fabric8.support.api.Resource;
import io.fabric8.support.api.ResourceFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import java.util.LinkedList;
import java.util.List;

/**
 * {@link io.fabric8.support.api.Collector} that collects information about the Fabric8 containers.
 */
@ThreadSafe
@Component(name = "io.fabric8.support.fabric8.commands", label = "Fabric8 Support - Fabric8 Commands Collector", metatype = false)
@Service(Collector.class)
public class CommandCollector implements Collector {

    @Reference(referenceInterface = FabricService.class)
    private FabricService fabricService;

    @Override
    public List<Resource> collect(ResourceFactory factory) {
        List<Resource> result = new LinkedList<>();

        result.add(factory.createCommandResource("fabric:container-list"));

        if(fabricService != null) {
            Container[] containers = fabricService.getContainers();
            for (Container c : containers){
                String name = c.getId();
                result.add(factory.createCommandResource("fabric:container-info " + name));
            }
        }
        result.add(factory.createCommandResource("zk:list -r -d"));
        //  result.add(new CommandResource("each [1 2 3 ] { echo \"================ Execution $it ==============\"; dev:threads --dump ;  sleep 5000 }"));

        result.add(factory.createCommandResource("fabric:camel:context-list"));
        return result;
    }
}
