/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.elasticsearch.discovery.zookeeper;

import org.elasticsearch.discovery.Discovery;
import org.elasticsearch.discovery.zen.ZenDiscoveryModule;

public class ZooKeeperDiscoveryModule extends ZenDiscoveryModule {

    @Override
    protected void bindDiscovery() {
        bind(Discovery.class).to(ZooKeeperDiscovery.class).asEagerSingleton();
    }
}
