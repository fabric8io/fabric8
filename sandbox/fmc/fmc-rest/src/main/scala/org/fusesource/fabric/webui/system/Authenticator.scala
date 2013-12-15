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

import com.sun.jersey.spi.resource.Singleton
import javax.security.auth.login.LoginContext
import javax.ws.rs.core.Context
import javax.servlet.ServletContext
import org.slf4j.{LoggerFactory, Logger}
import javax.security.auth.callback.{CallbackHandler, NameCallback, PasswordCallback, Callback}
import org.apache.karaf.jaas.config.JaasRealm
import org.apache.karaf.jaas.boot.principal.RolePrincipal
import org.apache.karaf.jaas.modules.{BackingEngineFactory, BackingEngine}
import java.util.HashMap
import org.osgi.framework.FrameworkUtil
import io.fabric8.webui.Services
import javax.security.auth.Subject

@Singleton
class Authenticator(@Context servletContext: ServletContext) {

  private val _bundle = FrameworkUtil.getBundle(getClass)
  private val _bundle_context = if (_bundle != null) _bundle.getBundleContext else null

  protected val logger : Logger = LoggerFactory getLogger getClass
  protected var domain: String = Services.realm

  protected var engine:BackingEngine = null
  protected var used_realm: JaasRealm = null

  def authenticate(username: String, password: String): Subject = {
    try {
      val callback = new LoginCallbackHandler(username, password)
      val lc = new LoginContext(domain, callback)
      lc.login()
      var subject:Subject = null;

      Option[Subject](lc.getSubject()) match {
        case Some(s) =>
          subject = s
        case None =>
          throw new Exception("Subject not set after calling LoginContext.login()");
      }

      var hasRole = false

      if (Services.role != null && !Services.role.equals("")) {
        subject.getPrincipals.toArray().foreach((x) => {
          x match {
           case rp:RolePrincipal =>
             if (rp.getName.equals(Services.role)) {
               hasRole = true
             }
           case _ =>
          }
        })
      } else {
        hasRole = true;
      }

      if (hasRole) {
        subject
      } else {
        null
      }
    } catch {
      case e:Throwable => {
        logger.info("Authentication failed", e)
        null
      }
    }
  }

  def auth_backing_engine:BackingEngine = {
    if (_bundle_context == null) {
      null
    } else if (engine != null) {
      engine
    } else {
      var services = _bundle_context.getServiceReferences(classOf[JaasRealm].getName, null)
      services.map(service => {
        val realm = _bundle_context.getService(service).asInstanceOf[JaasRealm]
        if (realm.getName == domain) {
          if (used_realm != null) {
            if (realm.getRank() > used_realm.getRank()) {
              used_realm = realm
            }
          } else {
            used_realm = realm
          }
        }
      })
      if (used_realm != null) {
        services = _bundle_context.getServiceReferences(classOf[BackingEngineFactory].getName, null)
        services.map(service => {
          val factory = _bundle_context.getService(service)
          if (factory.asInstanceOf[BackingEngineFactory].getModuleClass == used_realm.getEntries.head.getOptions.get("org.apache.karaf.jaas.module")) {
            engine = factory.asInstanceOf[BackingEngineFactory].build(used_realm.getEntries.head.getOptions)
          }
        })
      }
      engine
    }
  }

  class LoginCallbackHandler(username: String, password: String) extends CallbackHandler {

    def handle(callbacks: Array[Callback]) {
      for (callback <- callbacks) {
        if (callback.isInstanceOf[PasswordCallback]) {
          val passwordCallback: PasswordCallback = callback.asInstanceOf[PasswordCallback]
          if (password == null) {
            passwordCallback.setPassword(null)
          } else {
            passwordCallback.setPassword(password.toCharArray)
          }
        } else if (callback.isInstanceOf[NameCallback]) {
          val nameCallback: NameCallback = callback.asInstanceOf[NameCallback]
          if (username == null) {
            nameCallback.setName(null)
          } else {
            nameCallback.setName(username)
          }
        }

      }
    }

  }

}
