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

import org.fusesource.fabric.apollo.amqp.codec.types.{Role, Flow}


class LinkFlowControlTracker(val role:Role) {

  private var _link_credit = 0L
  private var _delivery_count = 0L
  private var _available = 0L
  private var _drain = false

  def track(func:(Boolean) => Unit) = {
    if (credit) {
      _available = _available + 1
      func(false)
    } else {
      advance_delivery_count
      if (_available > 0) {
        _available = _available - 1
      }
      func(true)
    }
  }

  def drain_link_credit:Unit = {
    if (_drain && _link_credit > 0) {
      advance_delivery_count
      drain_link_credit
    }
  }

  private def advance_delivery_count = {
    _link_credit = _link_credit - 1
    _delivery_count = _delivery_count + 1
  }

  def credit = _link_credit > 0

  def available = _available
  def available_=(a:Long) = _available = a
  
  def init_flow(flow:Flow = new Flow()):Flow = {
    flow.setDeliveryCount(_delivery_count)
    flow.setAvailable(_available)
    flow.setLinkCredit(_link_credit)
    flow.setDrain(_drain)
    flow
  }

  def flow(flow:Flow) = {
    role match {
      case Role.RECEIVER =>
        _delivery_count = Option[Long](flow.getDeliveryCount).getOrElse(0L)
        _available = Option[Long](flow.getAvailable).getOrElse(0L)
      case Role.SENDER =>
        _link_credit = Option[Long](flow.getLinkCredit).getOrElse(Long.MaxValue)
        _drain = Option[Boolean](flow.getDrain).getOrElse(false)
    }
  }

}