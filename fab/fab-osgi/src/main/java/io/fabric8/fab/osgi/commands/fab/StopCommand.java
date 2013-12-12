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
import org.apache.karaf.shell.osgi.Util;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(name = "stop", scope = "fab", description = "Stops the Fabric Bundle along with all of its transitive dependencies which are not being used by other bundles")
public class StopCommand extends ProcessUnusedBundles {
    private static final transient Logger LOG = LoggerFactory.getLogger(StopCommand.class);

    @Override
    protected void processBundle(Bundle bundle) throws Exception {
        if (Util.isASystemBundle(getBundleContext(), bundle) && !Util.accessToSystemBundleIsAllowed(bundle.getBundleId(), session)) {
           return;
        }

        stopBundle(bundle);
    }

}
