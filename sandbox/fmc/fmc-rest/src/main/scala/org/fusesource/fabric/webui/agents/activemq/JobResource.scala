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
import javax.ws.rs.{POST, Path}
import org.apache.activemq.broker.jmx._
import io.fabric8.activemq.facade.JobFacade
import io.fabric8.webui.BaseResource

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class JobResource(val self: JobFacade) extends BaseResource {

  @JsonProperty
  def cron_entry = self.getCronEntry

  @JsonProperty
  def delay = self.getDelay

  @JsonProperty
  def job_id = self.getJobId

  @JsonProperty
  def next_execution_time = self.getNextExecutionTime

  @JsonProperty
  def period = self.getPeriod

  @JsonProperty
  def repeat = self.getRepeat

  @JsonProperty
  def start = self.getStart

}
