/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.rest.main;

import io.fabric8.cdi.annotations.Service;
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

    @Inject
    public GitUserHelper(@Service(id ="GOGS_HTTP_SERVICE", protocol="http") String gogsUrl,
    @ConfigProperty(name = "GIT_DEFAULT_USER") String gitUser,
                         @ConfigProperty(name = "GIT_DEFAULT_PASSWORD") String gitPassword) {
        this.gitUser = gitUser;
        this.gitPassword = gitPassword;
        this.address = gogsUrl.toString();
        if (!address.endsWith("/")) {
            address += "/";
        }
    }

    public UserDetails createUserDetails(HttpServletRequest request) {
        String user = gitUser;
        String password = gitPassword;
        String authorization = request.getHeader("Authorization");
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
        String emailHeader = request.getHeader("Email");
        if (!Strings.isNullOrEmpty(emailHeader)) {
            email = emailHeader;
        }
        return new UserDetails(address, user, password, email);
    }
}
