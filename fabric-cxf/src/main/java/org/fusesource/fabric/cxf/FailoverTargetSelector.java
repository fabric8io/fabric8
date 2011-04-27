/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.cxf;

import org.apache.cxf.transport.Conduit;


public class FailoverTargetSelector extends LoadBalanceTargetSelector {

    public FailoverTargetSelector() {
        super(null);
    }

    public FailoverTargetSelector(Conduit c) {
        super(c);
    }





}
