/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.scr.InvalidComponentException;
import org.fusesource.fabric.api.scr.Validatable;
import org.fusesource.fabric.api.scr.ValidatingReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLStreamHandlerService;

@Component(name = "org.fusesource.fabric.profile.urlhandler", description = "Fabric Profile URL Handler", immediate = true)
@Service(URLStreamHandlerService.class)
@Properties({
        @Property(name = "url.handler.protocol", value = "profile")
})
public class ProfileUrlHandler extends AbstractURLStreamHandlerService implements Validatable {

    private static final String SYNTAX = "profile:<resource name>";

    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    private final AtomicBoolean active = new AtomicBoolean();

    @Activate
    synchronized void activate(ComponentContext context) {
        active.set(true);
    }

    @Deactivate
    synchronized void deactivate() {
        active.set(false);
    }

    @Override
    public synchronized boolean isValid() {
        return active.get();
    }

    @Override
    public synchronized void assertValid() {
        if (isValid() == false)
            throw new InvalidComponentException();
    }

    @Override
    public URLConnection openConnection(URL url) throws IOException {
        return new Connection(url);
    }

    public class Connection extends URLConnection {

        public Connection(URL url) throws MalformedURLException {
            super(url);
            if (url.getPath() == null || url.getPath().trim().length() == 0) {
                throw new MalformedURLException("Path can not be null or empty. Syntax: " + SYNTAX);
            }
            if ((url.getHost() != null && url.getHost().length() > 0) || url.getPort() != -1) {
                throw new MalformedURLException("Unsupported host/port in profile url");
            }
            if (url.getQuery() != null && url.getQuery().length() > 0) {
                throw new MalformedURLException("Unsupported query in profile url");
            }
        }

        @Override
        public void connect() throws IOException {
        }

        @Override
        public InputStream getInputStream() throws IOException {
            String path = url.getPath();
            Profile profile = fabricService.get().getCurrentContainer().getOverlayProfile();

            Map<String, byte[]> configs = profile.getFileConfigurations();
            if (configs.containsKey(path)) {
                byte[] b = configs.get(path);
                return new ByteArrayInputStream(b);
            } else {
                throw new IllegalArgumentException("Resource " + path + " does not exist in the profile overlay.");
            }
        }
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.set(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.set(null);
    }
}
