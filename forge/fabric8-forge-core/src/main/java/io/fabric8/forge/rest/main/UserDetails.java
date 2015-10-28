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
package io.fabric8.forge.rest.main;

import io.fabric8.repo.git.GitRepoClient;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 */
public class UserDetails {
    private static final transient Logger LOG = LoggerFactory.getLogger(UserDetails.class);

    private final String internalAddress;
    private final String user;
    private final String password;
    private final String address;
    private final String email;
    private String branch = "master";
    private File sshPrivateKey;
    private File sshPublicKey;

    public UserDetails(String address, String internalAddress, String user, String password, String email) {
        this.internalAddress = internalAddress;
        this.user = user;
        this.password = password;
        this.address = address;
        this.email = email;
    }


    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getUser() {
        return user;
    }

    /**
     * Returns the address of the gogs REST API
     */
    public String getAddress() {
        return address;
    }

    public String getInternalAddress() {
        return internalAddress;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public GitRepoClient createRepoClient() {
        LOG.info("creating git repository client at: " + internalAddress);
        return new GitRepoClient(internalAddress, user, password);
    }

    public CredentialsProvider createCredentialsProvider() {
        if (sshPrivateKey != null) {
            return new CredentialsProvider() {
                @Override
                public boolean isInteractive() {
                    return false;
                }

                @Override
                public boolean supports(CredentialItem... items) {
                    return true;
                }

                @Override
                public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
                        /*
                        for (CredentialItem item : items) {
                            ((CredentialItem.StringType) item).setValue("yourpassphrase");
                        }
                        */
                    return true;
                }
            };
        }
        return new UsernamePasswordCredentialsProvider(user, password);
    }

    public PersonIdent createPersonIdent() {
        return new PersonIdent(user, email);
    }

    public File getSshPrivateKey() {
        return sshPrivateKey;
    }

    public File getSshPublicKey() {
        return sshPublicKey;
    }

    public void setSshPrivateKey(File sshPrivateKey) {
        this.sshPrivateKey = sshPrivateKey;
    }

    public void setSshPublicKey(File sshPublicKey) {
        this.sshPublicKey = sshPublicKey;
    }
}
