/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.cxf;

import org.fusesource.fabric.groups.Group;

import java.util.List;

public interface LoadBalanceStrategy {
    void setGroup(Group group);
    List<String> getAlternateAddressList();
    String getNextAlternateAddress();
}
