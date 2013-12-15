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

package io.fabric8.fab.osgi.commands;

import org.apache.felix.gogo.commands.Argument;
import org.osgi.framework.Bundle;

public abstract class BundleCommandSupport extends CommandSupport {
    @Argument(index = 0, name = "id", description = "The bundle ID", required = true)
    Long id;


    @Override
    protected Object doExecute() throws Exception {
        // force lazy construction
        if (getPackageAdmin() == null) {
            return null;
        }
        Bundle bundle = getBundleContext().getBundle(id);
        if (bundle == null) {
            System.err.println("Bundle ID " + id + " is invalid");
            return null;
        }
        doExecute(bundle);
        return null;
    }

    protected abstract void doExecute(Bundle bundle) throws Exception;

}
