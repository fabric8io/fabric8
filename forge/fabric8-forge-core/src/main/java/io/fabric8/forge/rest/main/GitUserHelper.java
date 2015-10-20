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

import io.fabric8.annotations.External;
import io.fabric8.annotations.Protocol;
import io.fabric8.annotations.ServiceName;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.eclipse.jgit.util.Base64;
import org.jboss.forge.furnace.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.Charset;

/**
 * A helper class for working with git user stuff
 */
public class GitUserHelper {
    private static final transient Logger LOG = LoggerFactory.getLogger(GitUserHelper.class);
    private final String gitUser;
    private final String gitPassword;
    private String address;
    private String internalAddress;

    // TODO it'd be nice to pick either http or https based on the port number of the gogs service
    // so if folks configured it on https then we'd just work
    @Inject
    public GitUserHelper(@ServiceName("gogs") @Protocol("http") @External String gogsUrl,
                         @ServiceName("gogs") @Protocol("http") String gogsInternalUrl,
                         @ConfigProperty(name = "JENKINS_GOGS_USER") String gitUser,
                         @ConfigProperty(name = "JENKINS_GOGS_PASSWORD") String gitPassword) {
        this.gitUser = gitUser;
        this.gitPassword = gitPassword;
        this.address = gogsUrl;
        this.internalAddress = gogsInternalUrl;
        if (!address.endsWith("/")) {
            address += "/";
        }
        if (!internalAddress.endsWith("/")) {
            internalAddress += "/";
        }
    }

    public UserDetails createUserDetails(HttpServletRequest request) {
        String user = gitUser;
        String password = gitPassword;
        String authorization = null;
        String emailHeader = null;

        // lets try custom headers or request parameters
        if (request != null) {
            authorization = request.getHeader("GogsAuthorization");
            if (Strings.isNullOrEmpty(authorization)) {
                authorization = request.getParameter("_gogsAuth");
            }
            emailHeader = request.getHeader("GogsEmail");
            if (Strings.isNullOrEmpty(emailHeader)) {
                emailHeader = request.getParameter("_gogsEmail");
            }
        }
        if (!Strings.isNullOrEmpty(authorization)) {
            String basicPrefix = "basic";
            String lower = authorization.toLowerCase();
            if (lower.startsWith(basicPrefix)) {
                String base64Credentials = authorization.substring(basicPrefix.length()).trim();
                String credentials = new String(Base64.decode(base64Credentials),
                        Charset.forName("UTF-8"));
                // credentials = username:password
                String[] values = credentials.split(":", 2);
                if (values != null && values.length > 1) {
                    user = values[0];
                    password = values[1];
                }
            }
        }
        String email = "dummy@gmail.com";
        if (!Strings.isNullOrEmpty(emailHeader)) {
            email = emailHeader;
        }
        return new UserDetails(address, internalAddress, user, password, email);
    }
}
