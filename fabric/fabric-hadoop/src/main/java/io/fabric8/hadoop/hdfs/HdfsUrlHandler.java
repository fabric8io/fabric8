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
package io.fabric8.hadoop.hdfs;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Dictionary;
import java.util.Enumeration;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HdfsUrlHandler extends AbstractURLStreamHandlerService implements ManagedService {

    private static final Logger logger = LoggerFactory.getLogger(HdfsUrlHandler.class);

    private static final String SYNTAX = "hdfs:[path]";

    private Configuration conf;

    public HdfsUrlHandler() {
    }

    @Override
    public void updated(Dictionary properties) throws ConfigurationException {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            Configuration conf = new Configuration();
            if (properties != null) {
                for (Enumeration e = properties.keys(); e.hasMoreElements();) {
                    Object key = e.nextElement();
                    Object val = properties.get(key);
                    conf.set( key.toString(), val.toString() );
                }
            }
            this.conf = conf;
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    /**
     * Open the connection for the given URL.
     *
     * @param url the url from which to open a connection.
     * @return a connection on the specified URL.
     * @throws java.io.IOException if an error occurs or if the URL is malformed.
     */
    @Override
    public URLConnection openConnection(URL url) throws IOException {
        return new Connection(url);
    }

    public class Connection extends URLConnection {

        private InputStream is;

        public Connection(URL url) throws MalformedURLException {
            super(url);
        }

        @Override
        public void connect() throws IOException {
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                FileSystem fs = FileSystem.get(url.toURI(), conf);
                is = fs.open(new Path(url.getPath()));
            } catch (URISyntaxException e) {
                throw new IOException(e.toString());
            } finally {
                Thread.currentThread().setContextClassLoader(tccl);
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (is == null) {
                connect();
            }
            return is;
        }
    }
}
