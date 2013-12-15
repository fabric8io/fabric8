/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.fabric8.service.jclouds.functions;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import io.fabric8.service.jclouds.CreateJCloudsContainerOptions;
import org.jclouds.scriptbuilder.statements.login.AdminAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ToAdminAccess {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToAdminAccess.class);

    public static Optional<AdminAccess> apply(CreateJCloudsContainerOptions input) {
        AdminAccess.Builder builder = AdminAccess.builder();
        //There are images that have issues with copying of public keys, creation of admin user accounts,etc
        //To allow
        if (input.isAdminAccess()) {
            if (!Strings.isNullOrEmpty(input.getPublicKeyFile())) {
                File publicKey = new File(input.getPublicKeyFile());
                if (publicKey.exists()) {
                    builder.adminPublicKey(publicKey);
                } else {
                    LOGGER.warn("Public key has been specified file: {} files cannot be found. Ignoring.", publicKey.getAbsolutePath());
                    return Optional.of(AdminAccess.standard());
                }
            }

            if (!Strings.isNullOrEmpty(input.getUser())) {
                builder.adminUsername(input.getUser());
            }

            return Optional.of(builder.build());
        }
        return Optional.absent();
    }
}
