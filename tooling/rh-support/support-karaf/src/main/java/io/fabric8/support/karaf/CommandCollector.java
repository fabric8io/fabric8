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

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.support.api.Collector;
import io.fabric8.support.api.Resource;
import org.apache.felix.scr.ScrService;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.fusesource.jansi.AnsiOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

@ThreadSafe
@Component(name = "io.fabric8.support.karaf.commands", label = "Fabric8 Support - Karaf Commands Collector", metatype = false)
@Service(Collector.class)
public class CommandCollector implements Collector {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandCollector.class);
    private static final long DELAY = 5000l;

    private Executor executor = Executors.newSingleThreadExecutor();

    @Reference(referenceInterface = CommandProcessor.class)
    private CommandProcessor processor;

    @Reference(referenceInterface = ScrService.class)
    private ScrService scrService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, referenceInterface = FabricService.class)
    private FabricService fabricService;

    @Override
    public List<Resource> collect() {
        List<Resource> result = new LinkedList<>();
        result.add(new CommandResource("osgi:headers --force org.jboss.fuse.esb-commands | grep Bundle-Version"));
          //result.add(new CommandResource("osgi:headers --force io.fabric8.common-util | grep Bundle-Version"));

        result.add(new CommandResource("osgi:list -t 0"));
        result.add(new CommandResource("osgi:ls"));
        result.add(new CommandResource("osgi:headers"));
        result.add(new CommandResource("log:display"));
        result.add(new CommandResource("shell:info"));
        result.add(new CommandResource("packages:imports"));
        result.add(new CommandResource("packages:exports -i"));
        result.add(new CommandResource("dev:classloaders"));
        result.add(new CommandResource("dev:system-property"));
        result.add(new CommandResource("scr:list"));
        result.add(new CommandResource("jaas:realms"));
        result.add(new CommandResource("config:list"));
        result.add(new CommandResource("fabric:container-list"));

        if(fabricService != null) {
            Container[] containers = fabricService.getContainers();
            for (Container c : containers){
                String name = c.getId();
                result.add(new CommandResource("fabric:container-info " + name));
            }
        }
        result.add(new CommandResource("zk:list -r -d"));
        //  result.add(new CommandResource("each [1 2 3 ] { echo \"================ Execution $it ==============\"; dev:threads --dump ;  sleep 5000 }"));

        result.add(new CommandResource("fabric:camel:context-list"));

        result.add(new CommandResource("activemq:dstat"));
        result.add(new CommandResource("activemq:bstat"));

        for (final org.apache.felix.scr.Component component : scrService.getComponents()) {
            result.add(new CommandResource("scr:details " + component.getName()) {
                @Override
                public String getName() {
                    return super.getName() + "_" + component.getName();
                }
            });
        }
        return result;
    }

    private void executeCommand(final String command, final OutputStream os) {
        final PrintStream out = new PrintStream(new AnsiOutputStream(os));
        out.printf("Command: %s%n", command);
        out.printf("--------------------%n");
        final CommandSession session = processor.createSession(System.in, out, out);
        Future<Void> future = Executors.newSingleThreadExecutor().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    session.execute(command);
                } catch (Exception e) {
                    LOGGER.warn("Exception while collecting support information - error running command '{}'", command);
                    e.printStackTrace(out);
                }
                return null;
            }
        });

        try {
            future.get(DELAY, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOGGER.warn(String.format("Exception while collecting support information - error waiting for command '{}'", command), e);
            future.cancel(true);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private class CommandResource implements Resource {

        private final String command;

        public CommandResource(String command) {
            super();
            this.command = command;
        }

        @Override
        public void write(OutputStream os) {
            LOGGER.info("Adding output of command '{}' to support information (file name {})", command, getName());
            executeCommand(command, os);
        }

        @Override
        public String getName() {
            return String.format("commands/%s", command.replaceAll("\\|", "PIPE").replaceAll("[^a-zA-Z0-9-_\\.]", "_"));
        }
    }
}
