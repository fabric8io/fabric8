/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.jaas;


import org.apache.felix.utils.properties.Properties;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.ZKData;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class ZookeeperProperties extends Properties {

    protected String path;
    protected IZKClient zooKeeper;

    public ZookeeperProperties(IZKClient zooKeeper, String path) throws Exception {
        this.path = path;
        this.zooKeeper = zooKeeper;
        ZKData<String> zkData = zooKeeper.getZKStringData(path);
        String value = zkData.getData();
        if (value != null) {
            load(new StringReader(value));
        }
    }

    @Override
    public void save() throws IOException {
        StringWriter writer = new StringWriter();
        saveLayout(writer);
        try {
            zooKeeper.setData(path, writer.toString());
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
