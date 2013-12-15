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

package io.fabric8.webui

import org.codehaus.jackson.annotate.JsonProperty

/**
 *
 */


class PaginatedArrayDTO {

  @JsonProperty
  var page: Int = _

  @JsonProperty
  var per_page: Int = _

  @JsonProperty
  var total: Int = _

  @JsonProperty
  var models: Array[_] = _

}
