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
class AmqpCommandHandler(connection:ConnectionHandler, s:AmqpSession) extends AmqpHandler {

  val session = Option(s)
  def handleEmpty: Unit = {}
  def handleUnknown(body: AmqpType[_, _]): Unit = {}

  def handleProtocolHeader(protocolHeader: AmqpProtocolHeader) = connection.header(protocolHeader)
  def handleClose(close: AmqpClose) = connection.close
  def handleOpen(open: AmqpOpen) = connection.open(open)
  def handleSaslChallenge(saslChallenge: AmqpSaslChallenge) = connection.sasl_challenge(saslChallenge)
  def handleSaslResponse(saslResponse: AmqpSaslResponse) = connection.sasl_response(saslResponse)
  def handleSaslMechanisms(saslMechanisms: AmqpSaslMechanisms) = connection.sasl_mechanisms(saslMechanisms)
  def handleSaslInit(saslInit: AmqpSaslInit) = connection.sasl_init(saslInit)
  def handleSaslOutcome(saslOutcome: AmqpSaslOutcome) = connection.sasl_outcome(saslOutcome)

  def handleFlow(flow: AmqpFlow) = session.foreach((s) => s.flow(flow))
  def handleTransfer(transfer: AmqpTransfer) = session.foreach((s) => s.transfer(transfer))
  def handleDetach(detach: AmqpDetach) = session.foreach((s) => s.detach(detach))
  def handleDisposition(disposition: AmqpDisposition) = session.foreach((s) => s.disposition(disposition))
  def handleEnd(end: AmqpEnd) = session.foreach((s) => s.end(end))
  def handleBegin(begin: AmqpBegin) = session.foreach((s) => s.begin(begin))
  def handleAttach(attach: AmqpAttach) = session.foreach((s) => s.attach(attach))

}
