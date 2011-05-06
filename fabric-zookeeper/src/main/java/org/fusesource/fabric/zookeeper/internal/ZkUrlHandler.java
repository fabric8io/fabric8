/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.zookeeper.internal;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.linkedin.zookeeper.client.IZKClient;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZkUrlHandler extends AbstractURLStreamHandlerService {

    private static final Logger logger = LoggerFactory.getLogger(ZkUrlHandler.class);

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
        if (url.getPath() == null || url.getPath().trim().length() == 0) {
            throw new MalformedURLException("Path can not be null or empty. Syntax: " + SYNTAX );
        }
        if ((url.getHost() != null && url.getHost().length() > 0) || url.getPort() != -1) {
            throw new MalformedURLException("Unsupported host/port in zookeeper url");
        }
        if (url.getQuery() != null && url.getQuery().length() > 0) {
            throw new MalformedURLException("Unsupported query in zookeeper url");
        }
        return new Connection(url);
    }

    public class Connection extends URLConnection {

        public Connection(URL url) {
            super(url);
        }

        @Override
        public void connect() throws IOException {
        }

        @Override
        public InputStream getInputStream() throws IOException {
            try {
                return new ByteArrayInputStream(zooKeeper.getData(url.getPath()));
            } catch (Exception e) {
                logger.error("Error opening zookeeper url", e);
                throw (IOException) new IOException("Error opening zookeeper url").initCause(e);
            }
        }
    }

}
