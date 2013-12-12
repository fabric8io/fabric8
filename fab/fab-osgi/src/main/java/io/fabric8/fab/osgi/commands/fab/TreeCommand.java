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

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import io.fabric8.fab.osgi.internal.FabClassPathResolver;
import io.fabric8.fab.osgi.commands.CommandSupport;
import io.fabric8.fab.osgi.internal.FabResolverFactoryImpl;

/**
 * Shows the dependency tree of a maven artifact before it is deployed
 */
@Command(name = "tree", scope = "fab", description = "Display the dependency tree of a Fabric Bundle")
public class TreeCommand extends CommandSupport {
    @Argument(index = 0, name = "fab", description = "The Bundle ID, URL or file of the FAB", required = true)
    private String fab;

    @Override
    protected Object doExecute() throws Exception {
        FabResolverFactoryImpl.FabResolverImpl impl = getFabResolverImpl(fab);
        if (impl != null) {
            // call FabResolver.getInfo() to get FAB to resolve all dependencies
            impl.getInfo();

            FabClassPathResolver resolver = impl.getClassPathResolver();

            if (resolver != null) {
                TreeHelper.write(session.getConsole(), resolver);
            }
        }
        return null;
    }
}
