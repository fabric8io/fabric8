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

package io.fabric8.fab.osgi.commands.fab;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.fab.DependencyTree;
import io.fabric8.fab.osgi.internal.Bundles;
import io.fabric8.fab.osgi.internal.FabClassPathResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Command(name = "start", scope = "fab", description = "Starts the Fabric Bundle along with its transitive dependencies")
public class StartCommand extends FabCommandSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(StartCommand.class);

    @Option(name = "--timeout", description = "Maximum time to wait starting the FAB in milliseconds")
    private long timeout = 30000L;

    private transient long startTime;

    public void start(Bundle bundle) throws Exception {
        // force lazy construction
        getPackageAdmin();

        doExecute(bundle);
    }

    @Override
    protected void doExecute(Bundle bundle, FabClassPathResolver resolver) {
        // lets process the bundles from the deepest dependencies first
        List<DependencyTree> sharedDependencies = resolver.getSharedDependencies();
        for (int i = sharedDependencies.size() - 1; i >= 0; i--) {
            DependencyTree dependency = sharedDependencies.get(i);
            String name = dependency.getBundleSymbolicName();
            String version = dependency.getVersion();
            Bundle b = Bundles.findBundle(bundleContext, name, version);
            if (b != null) {
                startBundle(b);
            }
        }

        startBundle(bundle);
    }

    protected void startBundle(Bundle bundle) {
        int state = bundle.getState();
        if (state == Bundle.INSTALLED || state == Bundle.RESOLVED && !Bundles.isFragment(bundle)) {
            LOG.debug("Starting bundle %s version %s", bundle.getSymbolicName(), bundle.getVersion());
            try {
                bundle.start();

                if (startTime == 0L) {
                    startTime = System.currentTimeMillis();
                }
                // lets wait for it to start
                long end = startTime + timeout;
                while (true) {
                    state = bundle.getState();
                    if (state == Bundle.ACTIVE || state == Bundle.STOPPING || System.currentTimeMillis() > end) {
                        break;
                    }
                    try {
                        Thread.sleep(500L);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            } catch (BundleException e) {
                System.out.println("Failed to start " + bundle.getSymbolicName() + " " + bundle.getVersion() + ". " + e);
                e.printStackTrace();
            }
        }
    }
}