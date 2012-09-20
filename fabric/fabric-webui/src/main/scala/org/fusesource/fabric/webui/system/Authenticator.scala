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

package org.fusesource.fabric.webui.system

import com.sun.jersey.spi.resource.Singleton
import javax.security.auth.login.LoginContext
import javax.ws.rs.core.Context
import javax.servlet.ServletContext
import org.slf4j.{LoggerFactory, Logger}
import javax.security.auth.callback.{CallbackHandler, NameCallback, PasswordCallback, Callback}
import org.apache.karaf.jaas.config.JaasRealm
import org.apache.karaf.jaas.modules.{BackingEngineFactory, BackingEngine}
import java.util.HashMap
import org.osgi.framework.FrameworkUtil
import org.fusesource.fabric.jaas.{ZookeeperLoginModule, ZookeeperBackingEngineFactory}
import org.fusesource.fabric.webui.Services

@Singleton
class Authenticator(@Context servletContext: ServletContext) {

  private val _bundle = FrameworkUtil.getBundle(getClass)
  private val _bundle_context = if (_bundle != null) _bundle.getBundleContext else null

  protected val logger: Logger = LoggerFactory getLogger getClass
  val DEFAULT_DOMAIN = "karaf"
  protected var domain: String = DEFAULT_DOMAIN

  try {
    var path = System.getProperty("java.security.auth.login.config");
    if (path == null) {
      val resource = servletContext.getResource("/WEB-INF/login.config")
      if (resource != null) {
        path = resource.getFile();
        System.setProperty("java.security.auth.login.config", path);
      }
    }

    val zk = Services.zoo_keeper
    domain = zk.getStringData("fabric/authentication/domain")

  } catch {
    case e => logger.error("Error initializing authenticator", e)
  } finally {
    if (domain == null) {
      domain = DEFAULT_DOMAIN
    }
  }

  def authenticate(username: String, password: String): Boolean = {
    ZookeeperLoginModule.ZOOKEEPER_CONTEXT.set(Services.zoo_keeper)
    try {
      val callback = new LoginCallbackHandler(username, password);
      val lc = new LoginContext(domain, callback);
      lc.login();
      return true
    } catch {
      case e => {
        logger.info("Authentication failed", e)
        return false
      }
    } finally {
      ZookeeperLoginModule.ZOOKEEPER_CONTEXT.remove()
    }
  }

  private val _auth_backing_engine: BackingEngine = {
    if (_bundle_context == null) {
      createDefaultEngine
    } else {
      var services = _bundle_context.getServiceReferences(classOf[JaasRealm].getName, null)
      var engine: BackingEngine = null
      var used_realm: JaasRealm = null
      services.map(service => {
        val realm = _bundle_context.getService(service)
        if (realm.asInstanceOf[JaasRealm].getName == domain) {
          used_realm = realm.asInstanceOf[JaasRealm]
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
      if (engine == null) {
        engine = createDefaultEngine
      }
      engine
    }
  }

  def createDefaultEngine: BackingEngine = {
    val factory = new ZookeeperBackingEngineFactory();
    val map = new HashMap[Object, Object]();
    factory.setService(Services.fabric_service);
    map.put("encryption.enabled", "true")
    map.put("encryption.algorithm", "MD5")
    map.put("encryption.encoding", "hexadecimal")
    map.put("encryption.prefix", "{CRYPT}")
    map.put("encryption.suffix", "{CRYPT}")

    factory.build(map);
  }

  def auth_backing_engine = _auth_backing_engine

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
