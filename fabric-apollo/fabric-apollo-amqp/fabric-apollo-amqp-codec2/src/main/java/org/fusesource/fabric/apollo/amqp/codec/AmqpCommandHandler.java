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

public interface AmqpCommandHandler {

    public void handleFlow(Flow flow) throws Exception;

    public void handleClose(Close close) throws Exception;

    public void handleOpen(Open open) throws Exception;

    public void handleSASLOutcome(SASLOutcome saslOutcome) throws Exception;

    public void handleTransfer(Transfer transfer) throws Exception;

    public void handleDetach(Detach detach) throws Exception;

    public void handleSASLInit(SASLInit saslInit) throws Exception;

    public void handleSASLMechanisms(SASLMechanisms saslMechanisms) throws Exception;

    public void handleSASLResponse(SASLResponse saslResponse) throws Exception;

    public void handleDisposition(Disposition disposition) throws Exception;

    public void handleEnd(End end) throws Exception;

    public void handleBegin(Begin begin) throws Exception;

    public void handleSASLChallenge(SASLChallenge saslChallenge) throws Exception;

    public void handleAttach(Attach attach) throws Exception;
}