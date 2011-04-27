/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.api;

import java.util.List;

public interface ZooKeeperClusterService {

    List<String> getClusterAgents();

    String getZooKeeperUrl();

    void createCluster(List<String> agents);

    void addToCluster(List<String> agents);

    void removeFromCluster(List<String> agents);

    void clean();

}
