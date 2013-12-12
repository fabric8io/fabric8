/*
 * Copyright 2010 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.webui.agents.activemq

import org.codehaus.jackson.annotate.JsonProperty
import io.fabric8.activemq.facade.QueueViewFacade

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class QueueResource(override val self: QueueViewFacade)
  extends DestinationResource(self) {

  //  def copyMessageTo(p1: String, p2: String) = false
  //  def copyMatchingMessagesTo(p1: String, p2: String, p3: Int) = 0
  //  def copyMatchingMessagesTo(p1: String, p2: String) = 0
  //  def retryMessage(p1: String) = false
  //  def removeMessage(p1: String) = false
  //  def removeMatchingMessages(p1: String, p2: Int) = 0
  //  def removeMatchingMessages(p1: String) = 0
  //  def moveMessageTo(p1: String, p2: String) = false
  //  def moveMatchingMessagesTo(p1: String, p2: String, p3: Int) = 0
  //  def moveMatchingMessagesTo(p1: String, p2: String) = 0
  //  def getMessage(p1: String) = null

  @JsonProperty
  def id = self.getId

  //@JsonProperty
  //def purge = self.purge
  @JsonProperty
  def is_cursor_full = self.isCursorFull

  @JsonProperty
  def is_cache_enabled = self.isCacheEnabled

  @JsonProperty
  def get_cursor_percent_usage = self.getCursorPercentUsage

  @JsonProperty
  def get_cursor_memory_usage = self.getCursorMemoryUsage

  @JsonProperty
  def does_cursor_have_space = self.doesCursorHaveSpace

  @JsonProperty
  def does_cursor_have_messages_buffered = self.doesCursorHaveMessagesBuffered

  @JsonProperty
  def cursor_size = self.cursorSize


}
