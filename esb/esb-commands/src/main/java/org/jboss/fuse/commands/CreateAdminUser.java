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

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import io.fabric8.utils.shell.ShellUtils;

import java.io.File;
import java.io.IOException;

@Command(name = "create-admin-user", scope = "esb", description = "Creates a new admin user if one doesn't exist")
public class CreateAdminUser extends OsgiCommandSupport {

    @Option(name = "--new-user", multiValued = false, description = "The username of a new user. The option refers to karaf user (ssh, http, jmx).")
    private String newUser;
    @Option(name = "--new-user-password", multiValued = false, description = "The password of the new user. The option refers to karaf user (ssh, http, jmx).")
    private String newUserPassword;
    @Option(name = "--new-user-role", multiValued = false, description = "The role of the new user. The option refers to karaf user (ssh, http, jmx).")
    private String newUserRole = "admin";
    
    @Override
    protected Object doExecute() throws Exception {
        Properties userProps = new Properties(new File(System.getProperty("karaf.home") + "/etc/users.properties"));

        if (userProps.isEmpty()) {
            if (newUser == null || newUserPassword == null) { 
                String[] credentials = promptForNewUser(newUser, newUserPassword);
                newUser = credentials[0];
                newUserPassword = credentials[1];
            }       
        } 
        
        if (newUser == null) {
            System.out.println("No user specified. Some features may not be accessible.");
            return null;
        } else {            
            if (session != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("jaas:manage --realm karaf --index 1;")
                    .append("jaas:useradd ").append(newUser).append(" ").append(newUserPassword).append(";")
                    .append("jaas:roleadd ").append(newUser).append(" ").append(newUserRole).append(";")
                    .append("jaas:update");
                session.execute(sb.toString());
             }
        } 

        return null;
    }

    /**
     * Prompts the user for username and/or password.
     *
     * @param user     The default username.
     * @param password The default password.
     * @return An String array with username at index 0 and password at index 1.
     * @throws IOException
     */
    protected String[] promptForNewUser(String user, String password) throws IOException {
        String[] response = new String[2];
        // If the username was not configured, then prompt the user for the values
        if (user == null || password == null) {
            System.out.println("Please specify a user...");
        } 
        while (user == null || user.isEmpty()) {
            user = ShellUtils.readLine(session, "New user name: ", false);
            if (user == null) {
                break;
            }
        }

        if (user != null && password == null) {
            String password1 = null;
            String password2 = null;
            while (password1 == null || !password1.equals(password2)) {
                password1 = ShellUtils.readLine(session, "Password for " + user + ": ", true);
                password2 = ShellUtils.readLine(session, "Verify password for " + user + ": ", true);
                
                if (password1 == null || password2 == null) {
                    break;
                }
                
                if (password1 != null && password1.equals(password2)) {
                    password = password1;
                } else {
                    System.out.println("Passwords did not match. Please try again!");
                }
            }
        }
        response[0] = user;
        response[1] = password;
        return response;
    }
    
    public String getNewUser() {
        return newUser;
    }

    public void setNewUser(String newUser) {
        this.newUser = newUser;
    }

    public String getNewUserPassword() {
        return newUserPassword;
    }

    public void setNewUserPassword(String newUserPassword) {
        this.newUserPassword = newUserPassword;
    }

}
