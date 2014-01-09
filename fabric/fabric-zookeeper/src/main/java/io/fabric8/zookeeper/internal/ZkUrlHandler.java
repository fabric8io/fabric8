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
package io.fabric8.zookeeper.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.Validatable;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.api.scr.ValidationSupport;
import io.fabric8.zookeeper.ZkPath;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLStreamHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
@Component(name = "io.fabric8.zookeeper.urlhandler", label = "Fabric8 ZooKeeper URL Handler", immediate = true, metatype = false)
@Service(URLStreamHandlerService.class)
@Properties({ @Property(name = "url.handler.protocol", value = "zk") })
public class ZkUrlHandler extends AbstractURLStreamHandlerService implements Validatable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZkUrlHandler.class);

    private static final String SYNTAX = "zk: zk-node-path";

    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    private final ValidationSupport active = new ValidationSupport();

    @Activate
    void activate() {
        active.setValid();
    }

    @Deactivate
    void deactivate() {
        active.setInvalid();
    }

    @Override
    public boolean isValid() {
        return active.isValid();
    }

    @Override
    public void assertValid() {
        active.assertValid();
    }

    /**
     * Open the connection for the given URL.
     *
     * @param url the url from which to open a connection.
     * @return a connection on the specified URL.
     * @throws IOException if an error occurs or if the URL is malformed.
     */
    @Override
    public URLConnection openConnection(URL url) throws IOException {
        assertValid();
        return new Connection(url);
    }

    private class Connection extends URLConnection {

        public Connection(URL url) throws MalformedURLException {
            super(url);
            if (url.getPath() == null || url.getPath().trim().length() == 0) {
                throw new MalformedURLException("Path can not be null or empty. Syntax: " + SYNTAX);
            }
            if ((url.getHost() != null && url.getHost().length() > 0) || url.getPort() != -1) {
                throw new MalformedURLException("Unsupported host/port in zookeeper url");
            }
            if (url.getQuery() != null && url.getQuery().length() > 0) {
                throw new MalformedURLException("Unsupported query in zookeeper url");
            }
            if (url.getRef() != null) {
                String path = url.getPath().trim();
                if (!path.endsWith(".properties") && !path.endsWith(".json")) {
                    throw new MalformedURLException("Fragments are only supported for '.properties' and '.json' files.");
                }
            }
        }

        @Override
        public void connect() throws IOException {
            assertValid();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            assertValid();
            try {
                return new ByteArrayInputStream(ZkPath.loadURL(curator.get(), url.toString()));
            } catch (Exception e) {
                LOGGER.error("Error opening zookeeper url", e);
                throw (IOException) new IOException("Error opening zookeeper url").initCause(e);
            }
        }
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }
}
