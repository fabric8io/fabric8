/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 * 	http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.api;

import org.fusesource.fabric.apollo.amqp.codec.api.AnnotatedMessage;
import org.fusesource.fabric.apollo.amqp.codec.api.BareMessage;
import org.fusesource.fabric.apollo.amqp.codec.types.SenderSettleMode;
import org.fusesource.hawtbuf.Buffer;

/**
 *
 */
public interface Sender {

    public boolean full();

    public boolean offer(Buffer message);

    public boolean offer(AnnotatedMessage message);

    public boolean offer(BareMessage message);

    public void setTagger(DeliveryTagger tagger);

    public void setAckHandler(AckHandler handler);

    public void setSettleMode(SenderSettleMode mode);

    /**
     * Return a runnable that will be called by the Sender
     * when sufficient link credit is made available by
     * the peer
     *
     * @return
     */
    public Runnable refiller();
}
