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
package io.fabric8.jaas;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.encryption.EncryptionSupport;
import org.apache.karaf.jaas.modules.properties.PropertiesBackingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ZookeeperBackingEngine implements BackingEngine {

    public static final String USERS_NODE = "/fabric/authentication/users";

    private static final transient Logger LOGGER = LoggerFactory.getLogger(PropertiesBackingEngine.class);

    private Map<String, String> users;
    private EncryptionSupport encryptionSupport;

    public ZookeeperBackingEngine(Properties users) {
        this.users = users;
    }

    public ZookeeperBackingEngine(Properties users, EncryptionSupport encryptionSupport) {
        this.users = users;
        this.encryptionSupport = encryptionSupport;
    }

    /**
     * Add a User.
     */
    public void addUser(String username, String password) {
        String[] infos = null;
        StringBuffer userInfoBuffer = new StringBuffer();

        String newPassword = password;

        //If encryption support is enabled, encrypt password
        if (encryptionSupport != null && encryptionSupport.getEncryption() != null) {
            newPassword = encryptionSupport.getEncryption().encryptPassword(password);
            if (encryptionSupport.getEncryptionPrefix() != null) {
                newPassword = encryptionSupport.getEncryptionPrefix() + newPassword;
            }
            if (encryptionSupport.getEncryptionSuffix() != null) {
                newPassword = newPassword + encryptionSupport.getEncryptionSuffix();
            }
        }

        String userInfos = users.get(username);

        //If user already exists, update password
        if (userInfos != null && userInfos.length() > 0) {
            infos = userInfos.split(",");
            userInfoBuffer.append(newPassword);

            for (int i = 1; i < infos.length; i++) {
                userInfoBuffer.append(",");
                userInfoBuffer.append(infos[i]);
            }
            String newUserInfo = userInfoBuffer.toString();
            users.put(username, newUserInfo);
        } else {
            users.put(username, newPassword);
        }

        saveUserProperties();
    }

    /**
     * Delete a User.
     */
    public void deleteUser(String username) {
        users.remove(username);
        saveUserProperties();
    }

    /**
     * List Users
     */
    public List<UserPrincipal> listUsers() {
        List<UserPrincipal> result = new ArrayList<UserPrincipal>();

        for (String userNames :	users.keySet()) {
            UserPrincipal userPrincipal = new UserPrincipal(userNames);
            result.add(userPrincipal);
        }
        return result;
    }

    /**
     * List the Roles of the {@param user}
     */
    public List<RolePrincipal> listRoles(UserPrincipal user) {
        List<RolePrincipal> result = new ArrayList<RolePrincipal>();
        String userInfo = users.get(user.getName());
        String[] infos = userInfo.split(",");
        for (int i = 1; i < infos.length; i++) {
            result.add(new RolePrincipal(infos[i]));
        }
        return result;
    }

    /**
     * Add a role to a User.
     */
    public void addRole(String username, String role) {
        String userInfos = users.get(username);
        if (userInfos != null) {
            String newUserInfos = userInfos + "," + role;
            users.put(username, newUserInfos);
        }
        saveUserProperties();
    }

    /**
     * Delete a Role from the given User.
     */
    public void deleteRole(String username, String role) {
        String[] infos = null;
        StringBuffer userInfoBuffer = new StringBuffer();

        String userInfos = users.get(username);

        //If user already exists, remove the role
        if (userInfos != null && userInfos.length() > 0) {
            infos = userInfos.split(",");
            String password = infos[0];
            userInfoBuffer.append(password);

            for (int i = 1; i < infos.length; i++) {
                if (infos[i] != null && !infos[i].equals(role)) {
                    userInfoBuffer.append(",");
                    userInfoBuffer.append(infos[i]);
                }
            }
            String newUserInfo = userInfoBuffer.toString();
            users.put(username, newUserInfo);
        }

        saveUserProperties();
    }

    private void saveUserProperties() {
        try {
            ((Properties) users).save();
        } catch (Exception ex) {
            LOGGER.error("Cannot update users file,", ex);
        }
    }
}
