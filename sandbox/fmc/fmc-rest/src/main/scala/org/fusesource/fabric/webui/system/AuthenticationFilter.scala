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

package io.fabric8.webui.system

import com.sun.jersey.spi.container.{ContainerRequest, ContainerRequestFilter, ResourceFilter}
import javax.ws.rs.core.Context
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import javax.ws.rs.WebApplicationException
import java.io.UnsupportedEncodingException
import com.sun.jersey.core.util.Base64
import com.sun.jersey.api.core.ResourceContext
import javax.security.auth.Subject

class AuthenticationFilter extends ContainerRequestFilter {

  val HEADER_WWW_AUTHENTICATE: String = "WWW-Authenticate"
  val HEADER_AUTHORIZATION: String = "Authorization"
  val AUTHENTICATION_SCHEME_BASIC: String = "Basic"

  @Context
  var http_request: HttpServletRequest = null;

  @Context
  var http_response: HttpServletResponse = null;

  @Context
  var resource_context: ResourceContext = null;

  val LOGIN_URL = "/system/login"
  val LOGOUT_URL = "/system/logout"

  private def decode_base64(value: String): String = {
    var transformed: Array[Byte] = Base64.decode(value)
    try {
      return new String(transformed, "ISO-8859-1")
    } catch {
      case uee: UnsupportedEncodingException => {
        return new String(transformed)
      }
    }
  }

  def filter(request: ContainerRequest): ContainerRequest = {
    val session = http_request.getSession(false)
    val path = http_request.getServletPath()

    if (session != null
      || path == "/"
      || path == "/index.html"
      || path.startsWith("/favicon.")
      || path.startsWith("/img/")
      || path.startsWith("/styles/")
      || path.startsWith("/system/")
      || path.startsWith("/app/")
    ) {
      return request;
    } else {
      val auth_prompt = http_request.getHeader("AuthPrompt");

      if (session == null && auth_prompt != null && auth_prompt == "false") {
        throw new WebApplicationException(401);
      } else {
        var auth_header = http_request.getHeader(HEADER_AUTHORIZATION)
        if (auth_header != null && auth_header.length > 0) {
          auth_header = auth_header.trim
          var blank = auth_header.indexOf(' ')
          if (blank > 0) {
            var auth_type = auth_header.substring(0, blank)
            var auth_info = auth_header.substring(blank).trim
            if (auth_type.equalsIgnoreCase(AUTHENTICATION_SCHEME_BASIC)) {
              try {
                var srcString = decode_base64(auth_info)
                var i = srcString.indexOf(':')
                var username: String = srcString.substring(0, i)
                var password: String = srcString.substring(i + 1)

                val auth: Authenticator = resource_context.getResource(classOf[Authenticator]);

                Option[Subject](auth.authenticate(username, password)) match {
                  case Some(subject) =>
                    return request
                  case None =>
                }
              } catch {
                case e: Exception =>
              }
            }
          }
          if (session != null) {
            session.invalidate()
          }          
          throw new WebApplicationException(401)
        } else {
          val http_realm = "FON"
          http_response.addHeader(HEADER_WWW_AUTHENTICATE, AUTHENTICATION_SCHEME_BASIC + " realm=\"" + http_realm + "\"")
          throw new WebApplicationException(401);
        }
      }
    }


  }

}
