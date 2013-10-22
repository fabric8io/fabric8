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

import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.fabric.api.PlaceholderResolver;
import org.fusesource.fabric.api.jcip.ThreadSafe;
import org.fusesource.fabric.api.scr.AbstractComponent;
import org.fusesource.fabric.utils.ChecksumUtils;
import org.fusesource.fabric.utils.Closeables;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
@Component(name = "org.fusesource.fabric.placholder.resolver.checksum", description = "Fabric Checksum Placholder Resolver")
@Service(PlaceholderResolver.class)
public final class ChecksumPlaceholderResolver extends AbstractComponent implements PlaceholderResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChecksumPlaceholderResolver.class);
    private static final String CHECKSUM_SCHEME = "checksum";

    @Activate
    void activate(ComponentContext context) {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public String getScheme() {
        return CHECKSUM_SCHEME;
    }

    @Override
    public String resolve(Map<String, Map<String, String>> configs, String pid, String key, String value) {
        assertValid();
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
