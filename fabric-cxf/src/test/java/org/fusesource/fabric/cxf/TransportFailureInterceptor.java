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
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

import java.io.IOException;

public class TransportFailureInterceptor extends AbstractPhaseInterceptor<Message> {

    public TransportFailureInterceptor() {
        super(Phase.POST_STREAM);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        String address = (String) message.get(Message.ENDPOINT_ADDRESS);
        System.out.println(" address is " + address);
        if (address.indexOf("fail") > 0) {
            Exception ex = new IOException("Throw the IOException for address " + address);
            message.setContent(Exception.class, ex);
            throw new Fault(ex);
        }
    }
}
