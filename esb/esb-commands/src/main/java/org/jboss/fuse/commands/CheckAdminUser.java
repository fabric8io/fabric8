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
package org.jboss.fuse.commands;

import java.io.File;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.shell.console.OsgiCommandSupport;

@Command(name = "check-for-user", scope = "esb", description = "Checks if any users are defined.")
public class CheckAdminUser extends OsgiCommandSupport {
    
    @Override
    protected Object doExecute() throws Exception {
        Properties userProps = new Properties(new File(System.getProperty("karaf.home") + "/etc/users.properties"));

        if (userProps.isEmpty()) {
            System.out.println("No user found in etc/users.properties. Please use the 'esb:create-admin-user'");
            System.out.println("command to create one.");
            System.out.println();
        } 

        return null;
    }

}
