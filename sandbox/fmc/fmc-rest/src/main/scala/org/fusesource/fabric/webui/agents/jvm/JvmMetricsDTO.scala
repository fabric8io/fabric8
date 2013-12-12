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
package io.fabric8.webui.agents.jvm

import org.codehaus.jackson.annotate.JsonProperty

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */

class JvmMetricsDTO {
  @JsonProperty
  var heap_memory: MemoryMetricsDTO = null
  @JsonProperty
  var non_heap_memory: MemoryMetricsDTO = null
  @JsonProperty
  var classes_loaded: Int = 0
  var classes_unloaded: Long = 0L
  @JsonProperty
  var threads_current: Int = 0
  @JsonProperty
  var threads_peak: Int = 0
  @JsonProperty
  var os_arch: String = null
  @JsonProperty
  var os_name: String = null
  @JsonProperty
  var os_memory_total: Long = 0L
  @JsonProperty
  var os_memory_free: Long = 0L
  @JsonProperty
  var os_swap_total: Long = 0L
  @JsonProperty
  var os_swap_free: Long = 0L
  @JsonProperty
  var os_fd_open: Long = 0L
  @JsonProperty
  var os_fd_max: Long = 0L
  @JsonProperty
  var os_load_average: Double = .0
  @JsonProperty
  var os_cpu_time: Long = 0L
  @JsonProperty
  var os_processors: Int = 0

  @JsonProperty
  var runtime_name: String = null

  @JsonProperty
  var jvm_name: String = null

  @JsonProperty
  var spec_name: String = null

  @JsonProperty
  var spec_vendor: String = null

  @JsonProperty
  var spec_version: String = null

  @JsonProperty
  var vm_name: String = null

  @JsonProperty
  var vm_vendor: String = null

  @JsonProperty
  var vm_version: String = null


  @JsonProperty
  var uptime: Long = 0L
  @JsonProperty
  var start_time: Long = 0L
}
