/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.protocol

import org.fusesource.fabric.apollo.amqp.codec.AmqpHandler
import org.fusesource.fabric.apollo.amqp.codec.AmqpProtocolHeader
import org.fusesource.fabric.apollo.amqp.codec.types._

/**
 *
 */
class AmqpCommandHandler(connection:ConnectionHandler, session:AmqpSession) extends AmqpHandler {

  def handleProtocolHeader(protocolHeader: AmqpProtocolHeader): Unit = connection.header(protocolHeader)

  def handleEmpty: Unit = {}

  def handleUnknown(body: AmqpType[_, _]): Unit = {}

  def handleFlow(flow: AmqpFlow): Unit = {
    if ( session != null ) {
      session.flow(flow)
    }
  }

  def handleClose(close: AmqpClose): Unit = {
    connection.close
  }

  def handleOpen(open: AmqpOpen): Unit = {
    connection.open(open)
  }

  def handleTransfer(transfer: AmqpTransfer): Unit = {
    if ( session != null ) {
      session.transfer(transfer)
    }
  }

  def handleDetach(detach: AmqpDetach): Unit = {
    if ( session != null ) {
      session.detach(detach)
    }
  }

  def handleDisposition(disposition: AmqpDisposition): Unit = {
    if ( session != null ) {
      session.disposition(disposition)
    }
  }

  def handleEnd(end: AmqpEnd): Unit = {
    if ( session != null ) {
      session.end(end)
    }
  }

  def handleBegin(begin: AmqpBegin): Unit = {
    if ( session != null ) {
      session.begin(begin)
    }
  }

  def handleAttach(attach: AmqpAttach): Unit = {
    if ( session != null ) {
      session.attach(attach)
    }
  }

  def handleSaslChallenge(saslChallenge: AmqpSaslChallenge) {}

  def handleSaslResponse(saslResponse: AmqpSaslResponse) {}

  def handleSaslMechanisms(saslMechanisms: AmqpSaslMechanisms) {}

  def handleSaslInit(saslInit: AmqpSaslInit) {}

  def handleSaslOutcome(saslOutcome: AmqpSaslOutcome) {}
}
