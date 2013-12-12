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
import javax.ws.rs.{DELETE, Path}
import io.fabric8.activemq.facade.DurableSubscriptionViewFacade

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class DurableSubscriptionResource(override val self: DurableSubscriptionViewFacade)
  extends SubscriptionResource(self) {

  @JsonProperty
  def does_cursor_have_space = self.doesCursorHaveSpace

  @JsonProperty
  def does_cursor_have_messages_buffered = self.doesCursorHaveMessagesBuffered

  @JsonProperty
  def cursor_full = self.isCursorFull

  @JsonProperty
  def cursor_size = self.cursorSize()

  @JsonProperty
  def cursor_memory_usage = self.getCursorMemoryUsage

  @JsonProperty
  def cursor_percent_usage = self.getCursorPercentUsage

  @DELETE
  @Path("destroy")
  def destroy: Unit = self.destroy

}
