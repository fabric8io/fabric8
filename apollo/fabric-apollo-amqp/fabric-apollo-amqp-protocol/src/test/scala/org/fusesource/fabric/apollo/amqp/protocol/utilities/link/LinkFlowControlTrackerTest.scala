/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
