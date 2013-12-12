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

package io.fabric8.monitor.api

import io.fabric8.service.JmxTemplateSupport

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
trait Monitor {

  /**
   * Updates the monitor's configuration with the data sources that need
   * to be monitored.
   */
  def configure( value:Traversable[MonitoredSetDTO] ):Unit

  def close:Unit

  def fetch( fetch:FetchMonitoredViewDTO ):Option[MonitoredViewDTO]

  def list: Array[MonitoredSetDTO]

  var poller_factories:Seq[PollerFactory]
}
