/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.utilities.link

import org.fusesource.fabric.apollo.amqp.codec.types.Flow

class LinkFlowControlTracker {

  var link_credit = 0L
  var delivery_count = 0L
  var available = 0L
  var drain = false

  def track(credit: => Unit)(no_credit: => Unit) = {
    if (link_credit <= 0) {
      available = available + 1
      no_credit
    } else {
      advance_delivery_count
      if (available > 0) {
        available = available - 1
      }
      credit
    }
  }
  
  def drain_link_credit:Unit = {
    if (drain && link_credit > 0) {
      advance_delivery_count
      drain_link_credit
    }
  }

  private def advance_delivery_count = {
    link_credit = link_credit - 1
    delivery_count = delivery_count + 1
  }
  
  def init_flow(flow:Flow = new Flow()):Flow = {
    flow.setDeliveryCount(delivery_count)
    flow.setAvailable(available)
    flow.setLinkCredit(link_credit)
    flow.setDrain(drain)
    flow
  }

  def receiver_flow(flow:Flow) = {
    delivery_count = flow.getDeliveryCount
    available = flow.getAvailable

  }

  def sender_flow(flow:Flow) = {
    link_credit = flow.getLinkCredit
    drain = flow.getDrain
  }


}