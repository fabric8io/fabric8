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

import org.scalatest.matchers.ShouldMatchers
import org.apache.activemq.apollo.util.FunSuiteSupport
import org.fusesource.fabric.apollo.amqp.codec.types.{Role, Flow}

class LinkFlowControlTrackerTest extends FunSuiteSupport with ShouldMatchers {
  
  ignore("Track with no link credit") {
    val tracker = new LinkFlowControlTracker(Role.SENDER)
    var have_no_credit = false
    var visited = false
    tracker.track((credit) => {
      if (credit) {
        visited = true
      } else {
        have_no_credit = true
      }
    })
    have_no_credit should be (true)
    visited should be (false)
  }
  
  ignore("Track with link credit") {
    val tracker = new LinkFlowControlTracker(Role.SENDER)
    tracker.flow(new Flow(0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, false))
    var have_no_credit = false
    var visited = false
    tracker.track( (credit) => {
      if (credit) {
        visited = true
      } else {
        have_no_credit = true
      }
    })
    have_no_credit should be (false)
    visited should be (true)
    tracker.init_flow().getLinkCredit should be (0L)
    tracker.init_flow().getDeliveryCount should be (1L)
    tracker.available should be (0L)
  }
  
  ignore("Track several units, run out of link credit") {
    val tracker = new LinkFlowControlTracker(Role.SENDER)
    tracker.flow(new Flow(0L, 0L, 0L, 0L, 0L, 0L, 5L, 0L, false))
    var sent = 0
    var not_sent = 0
    def go(i:Int,  max:Int): Unit = {
      tracker.track( (credit) => {
        if (credit) {
          sent = sent + 1
        } else {
          not_sent = not_sent + 1
        }
      })
      if (i < max) {
        go(i + 1, max)
      }
    }
    go(1, 10)
    sent should be (5)
    not_sent should  be (5)
    tracker.init_flow().getLinkCredit should be (0L)
    tracker.init_flow().getDeliveryCount should be (5L)
    tracker.available should be (5L)
  }

}
