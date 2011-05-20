/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.cxf;

import org.apache.cxf.interceptor.Fault;

import java.io.IOException;

public class HelloImplFail implements Hello {

    public String sayHello() {
        throw new Fault(new IOException("It's an IOException."));
    }

    public void ping() {
        System.out.println("Call ping method");
    }

}
