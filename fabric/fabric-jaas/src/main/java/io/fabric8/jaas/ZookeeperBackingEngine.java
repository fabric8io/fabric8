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
package io.fabric8.jaas;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.encryption.EncryptionSupport;
import org.apache.karaf.jaas.modules.properties.PropertiesBackingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        if (username.startsWith(GROUP_PREFIX)) {
            throw new IllegalArgumentException("Prefix not permitted: " + GROUP_PREFIX);
        }
        addUserInternal(username, password);
    }

    private void addUserInternal(String username, String password) {
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
        // delete all its groups first, for garbage collection of the groups
        for (GroupPrincipal gp : listGroups(username)) {
            deleteGroup(username, gp.getName());
        }

        users.remove(username);
        saveUserProperties();
    }

    /**
     * List Users
     */
    public List<UserPrincipal> listUsers() {
        List<UserPrincipal> result = new ArrayList<UserPrincipal>();

        for (String userName :	users.keySet()) {
            if (userName.startsWith(GROUP_PREFIX)) {
                continue;
            }

            UserPrincipal userPrincipal = new UserPrincipal(userName);
            result.add(userPrincipal);
        }
        return result;
    }

    /**
     * List the Roles of the {@param user}
     */
    public List<RolePrincipal> listRoles(Principal principal) {
        String userName = principal.getName();
        if (principal instanceof GroupPrincipal) {
            userName = GROUP_PREFIX + userName;
        }
        return listRoles(userName);
    }

    private List<RolePrincipal> listRoles(String name) {
        List<RolePrincipal> result = new ArrayList<RolePrincipal>();
        String userInfo = users.get(name);
        String[] infos = userInfo.split(",");
        for (int i = 1; i < infos.length; i++) {
            String roleName = infos[i];
            if (roleName.startsWith(GROUP_PREFIX)) {
                for (RolePrincipal rp : listRoles(roleName)) {
                    if (!result.contains(rp)) {
                        result.add(rp);
                    }
                }
            } else {
                RolePrincipal rp = new RolePrincipal(roleName);
                if (!result.contains(rp)) {
                    result.add(rp);
                }
            }
        }
        return result;
    }

    /**
     * Add a role to a User.
     */
    public void addRole(String username, String role) {
        String userInfos = users.get(username);
        if (userInfos != null) {
            for (RolePrincipal rp : listRoles(username)) {
                if (role.equals(rp.getName())) {
                    return;
                }
            }
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

       @Override
       public List<GroupPrincipal> listGroups(UserPrincipal user) {
               String userName = user.getName();
               return listGroups(userName);
           }
   
               private List<GroupPrincipal> listGroups(String userName) {
               List<GroupPrincipal> result = new ArrayList<GroupPrincipal>();
               String userInfo = (String) users.get(userName);
               if (userInfo != null) {
                       String[] infos = userInfo.split(",");
                       for (int i = 1; i < infos.length; i++) {
                               String name = infos[i];
                               if (name.startsWith(GROUP_PREFIX)) {
                                       result.add(new GroupPrincipal(name.substring(GROUP_PREFIX.length())));
                                   }
                           }
                   }
               return result;
           }
   
               @Override
       public void addGroup(String username, String group) {
               String groupName = GROUP_PREFIX + group;
               if (users.get(groupName) == null) {
                       addUserInternal(groupName, "group");
                   }
               addRole(username, groupName);
           }
   
               @Override
       public void deleteGroup(String username, String group) {
               deleteRole(username, GROUP_PREFIX + group);
       
                       // garbage collection, clean up the groups if needed
                               for (UserPrincipal user : listUsers()) {
                       for (GroupPrincipal g : listGroups(user)) {
                               if (group.equals(g.getName())) {
                                       // there is another user of this group, nothing to clean up
                                               return;
                                   }
                           }
                   }
       
                       // nobody is using this group any more, remote it
                               deleteUser(GROUP_PREFIX + group);
           }
   
               @Override
       public void addGroupRole(String group, String role) {
               addRole(GROUP_PREFIX + group, role);
           }
   
               @Override
       public void deleteGroupRole(String group, String role) {
               deleteRole(GROUP_PREFIX + group, role);
           }
   
           
    private void saveUserProperties() {
        try {
            ((Properties) users).save();
        } catch (Exception ex) {
            LOGGER.error("Cannot update users file,", ex);
        }
    }
}
