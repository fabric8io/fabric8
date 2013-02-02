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
package org.fusesource.fabric.zookeeper.internal;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZkUrlHandler extends AbstractURLStreamHandlerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZkUrlHandler.class);

    private static final String SYNTAX = "zk: zk-node-path";

    private IZKClient zooKeeper;

    public ZkUrlHandler() {
    }

    public ZkUrlHandler(IZKClient zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    public IZKClient getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(IZKClient zooKeeper) {
        this.zooKeeper = zooKeeper;
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
        return new Connection(url);
    }

    public class Connection extends URLConnection {

        public Connection(URL url) throws MalformedURLException {
            super(url);
            if (url.getPath() == null || url.getPath().trim().length() == 0) {
                throw new MalformedURLException("Path can not be null or empty. Syntax: " + SYNTAX );
            }
            if ((url.getHost() != null && url.getHost().length() > 0) || url.getPort() != -1) {
                throw new MalformedURLException("Unsupported host/port in zookeeper url");
            }
            if (url.getQuery() != null && url.getQuery().length() > 0) {
                throw new MalformedURLException("Unsupported query in zookeeper url");
            }
            if( url.getRef()!=null ) {
                String path = url.getPath().trim();
                if( !path.endsWith(".properties") && !path.endsWith(".json") ) {
                    throw new MalformedURLException("Fragments are only supported for '.properties' and '.json' files.");
                }
            }
        }

        @Override
        public void connect() throws IOException {
        }

        @Override
        public InputStream getInputStream() throws IOException {
            try {
              return new ByteArrayInputStream(ZkPath.loadURL(zooKeeper, url.toString()));
            } catch (Exception e) {
                LOGGER.error("Error opening zookeeper url", e);
                throw (IOException) new IOException("Error opening zookeeper url").initCause(e);
            }
        }
    }

}
