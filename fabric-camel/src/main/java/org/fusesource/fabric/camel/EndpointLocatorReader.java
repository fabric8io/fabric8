/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel;

import org.apache.camel.util.ObjectHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.ZKData;
import org.linkedin.zookeeper.tracker.ZKDataReader;

import java.util.ArrayList;
import java.util.List;

public class EndpointLocatorReader implements ZKDataReader<EndpointLocator> {
    private static final transient Log LOG = LogFactory.getLog(EndpointLocatorReader.class);

    public ZKData<EndpointLocator> readData(IZKClient client, String path, Watcher watcher) throws InterruptedException, KeeperException {
        Stat stat = new Stat();
        List<String> children = client.getChildren(path, true, stat);
        List<String> uris = new ArrayList<String>();
        for (String child : children) {
            String uri = client.getStringData(path + "/" + child);
            uris.add(uri);
        }
        LOG.info("Children of path: " + path + " are now: " + children + " uris: " + uris);

        return new ZKData<EndpointLocator>(new EndpointLocator(uris), stat);
    }

    public boolean isEqual(EndpointLocator endpoint, EndpointLocator endpoint2) {
        return ObjectHelper.equal(endpoint, endpoint2);
    }
}
