/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.commands;

import com.google.common.base.Charsets;
import io.fabric8.api.*;
import io.fabric8.utils.PasswordEncoder;
import io.fabric8.zookeeper.ZkPath;
import io.fabric8.zookeeper.curator.CuratorACLManager;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;

@Command(name = EnsemblePassword.FUNCTION_VALUE, scope = EnsemblePassword.SCOPE_VALUE, description = EnsemblePassword.DESCRIPTION, detailedDescription = "classpath:ensemblePassword.txt")
public class EnsemblePasswordAction extends AbstractAction {

    private final FabricService fabricService;

    @Argument(index = 0, name = "new-password", description = "The new ensemble password.  If not supplied, the password is displayed.")
    private String newPassword;

    @Option(name = "-c", aliases = {"--commit"}, multiValued = false, required = false, description = "Commits the password change.")
    protected boolean commit = false;

    EnsemblePasswordAction(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    @Override
    protected Object doExecute() throws Exception {
        if( !commit ) {

            if( newPassword==null ) {
                System.out.println(fabricService.getZookeeperPassword());
            } else {
                String zookeeperUrl = fabricService.getZookeeperUrl();
                String oldPassword = fabricService.getZookeeperPassword();

                System.out.println("Updating the password...");

                // Since we will be changing the password, create a new ZKClient that won't
                // be getting update by the password change.
                CuratorACLManager aclManager = new CuratorACLManager();

                CuratorFramework curator = CuratorFrameworkFactory.builder().connectString(zookeeperUrl).retryPolicy(new RetryOneTime(500))
                        .aclProvider(aclManager).authorization("digest", ("fabric:" + oldPassword).getBytes()).sessionTimeoutMs(30000)
                        .build();
                curator.start();
                try {

                    // Lets first adjust the acls so that the new password and old passwords work against the ZK paths.
                    String digestedIdPass = DigestAuthenticationProvider.generateDigest("fabric:" + newPassword);
                    aclManager.registerAcl("/fabric", "auth::acdrw,world:anyone:,digest:" + digestedIdPass + ":acdrw");
                    aclManager.fixAcl(curator, "/fabric", true);

                    // Ok now lets push out a config update of what the password is.
                    curator.setData().forPath(ZkPath.CONFIG_ENSEMBLE_PASSWORD.getPath(), PasswordEncoder.encode(newPassword).getBytes(Charsets.UTF_8));
                } finally {
                    curator.close();
                }

                // Refresh the default profile to cause all nodes to pickup the new password.
                ProfileService profileService = fabricService.adapt(ProfileService.class);
                for (String ver : profileService.getVersions()) {
                    Version version = profileService.getVersion(ver);
                    if (version != null) {
                        Profile profile = version.getProfile("default");
                        if (profile != null) {
                            Profiles.refreshProfile(fabricService, profile);
                        }
                    }
                }

                System.out.println("");
                System.out.println("Password updated. Please wait a little while for the new password to");
                System.out.println("get delivered as a config update to all the fabric nodes. Once, the ");
                System.out.println("nodes all updated (nodes must be online), please run:");
                System.out.println("");
                System.out.println("  fabric:ensemble-password --commit ");
                System.out.println("");

            }
        } else {

            // Now lets connect with the new password and reset the ACLs so that the old password
            // does not work anymore.
            CuratorACLManager aclManager = new CuratorACLManager();
            CuratorFramework curator = CuratorFrameworkFactory.builder().connectString(fabricService.getZookeeperUrl()).retryPolicy(new RetryOneTime(500))
                    .aclProvider(aclManager).authorization("digest", ("fabric:" + fabricService.getZookeeperPassword()).getBytes()).sessionTimeoutMs(30000)
                    .build();
            curator.start();
            try {
                aclManager.fixAcl(curator, "/fabric", true);
                System.out.println("Only the current password is allowed access to fabric now.");
            } finally {
                curator.close();
            }
        }
        return null;
    }

}
