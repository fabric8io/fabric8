/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.codec;

import org.fusesource.fabric.apollo.amqp.codec.types.*;
import org.fusesource.hawtbuf.Buffer;
import org.junit.Test;

import static org.fusesource.fabric.apollo.amqp.codec.TestSupport.writeRead;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class DescribedTypeTest {

    @Test
    public void testOpen() throws Exception {
        Open in = new Open();
        in.setChannelMax((int)Short.MAX_VALUE);
        in.setContainerID("foo");
        in.setHostname("localhost");
        in.setOfferedCapabilities(new AMQPSymbol[]{new AMQPSymbol(new Buffer("blah".getBytes()))});
        Open out = writeRead(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testBegin() throws Exception {
        Begin in = new Begin();
        in.setHandleMax((long)Integer.MAX_VALUE);
        in.setIncomingWindow(10L);
        in.setOutgoingWindow(10L);
        Begin out = writeRead(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testEnd() throws Exception {
        End in = new End();
        End out = writeRead(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testAttach() throws Exception {
        Attach in = new Attach();
        in.setName("TEST");
        in.setHandle(0L);
        in.setRole(Role.SENDER.getValue());
        in.setInitialDeliveryCount(0L);
        Source source = new Source();
        Target target = new Target();
        target.setAddress(new AddressString("Foo"));
        in.setSource(source);
        in.setTarget(target);

        Attach out = writeRead(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testDetach() throws Exception {
        Detach in = new Detach();
        in.setClosed(true);
        Detach out = writeRead(in);
        assertEquals(in.toString(), out.toString());
    }
}
