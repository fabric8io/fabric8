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

import com.sun.jersey.spi.container.{ContainerRequest, ContainerRequestFilter}

/**
 * Request filter which accept both - X-HTTP-Method-Override header and _method post paremter.
 *
 * @author ldywicki
 */
class HiddenHttpMethodFilter extends ContainerRequestFilter {

  def filter(request: ContainerRequest): ContainerRequest = {
    if (!request.getMethod.equalsIgnoreCase("POST"))
      request

    val headerOver = request.getRequestHeaders.getFirst("X-HTTP-Method-Override")
    val paramOver = request.getFormParameters.getFirst("_method")

    def solve(_method: String) = {
      val method = _method.trim();
      if (!method.isEmpty) {
        request.setMethod(method)
      }
    }

    if (paramOver != null) {
      solve(paramOver)
    }
    if (headerOver != null) {
      solve(headerOver)
    }
    request
  }


}
