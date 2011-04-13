/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.zookeeper.commands;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;

@Command(name = "import", scope = "zk", description = "Import data into zookeeper")
public class Import extends ZooKeeperCommandSupport {

    @Argument(description = "URL of the file to load")
    String url;

    @Override
    protected Object doExecute() throws Exception {
        InputStream in = new BufferedInputStream(new URL(url).openStream());
        try {
            Properties props = new Properties();
            props.load(in);
            for (Enumeration names = props.propertyNames(); names.hasMoreElements();) {
                String name = (String) names.nextElement();
                String value = props.getProperty(name);
                if (value != null && value.isEmpty()) {
                    value = null;
                }
                getZooKeeper().createOrSetWithParents(name, value, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } finally {
            in.close();
        }
        return null;
    }

}
