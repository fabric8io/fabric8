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
package org.fusesource.fabric.service;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.fabric.api.PlaceholderResolver;
import org.fusesource.fabric.utils.ChecksumUtils;
import org.fusesource.fabric.utils.Closeables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;

@Component(name = "org.fusesource.fabric.placholder.resolver.checksum",
           description = "Fabric Checksum Placholder Resolver")
@Service(PlaceholderResolver.class)
public class ChecksumPlaceholderResolver implements PlaceholderResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChecksumPlaceholderResolver.class);
    private static final String CHECKSUM_SCHEME = "checksum";

    @Override
    public String getScheme() {
        return CHECKSUM_SCHEME;
    }

    @Override
    public String resolve(String pid, String key, String value) {
        InputStream is = null;
        try {
            URL url = new URL(value.substring("checksum:".length()));
            is = url.openStream();
            return String.valueOf(ChecksumUtils.checksum(is));
        } catch (Exception ex) {
            LOGGER.debug("Could not ");
            return "0";
        } finally {
            Closeables.closeQuitely(is);
        }
    }
}
