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

import org.eclipse.jetty.server.{Connector, Server}
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.server.nio.SelectChannelConnector
import java.io.File
import java.lang.String
import java.net.URI
import util.FileSupport

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object IDERunner {

  def main(args: Array[String]): Unit = {

    import FileSupport._

    // lets set the system properties to dev mode
    System.setProperty("scalate.mode", "dev")

    val bind = "http://0.0.0.0:8282/fmc"
    val bind_uri = new URI(bind)
    val prefix = "/" + bind_uri.getPath.stripPrefix("/")
    val host = bind_uri.getHost
    val port = bind_uri.getPort

    var connector = new SelectChannelConnector
    connector.setHost(host)
    connector.setPort(port)

    def webapp: File = {

      // the war might be on the classpath..
      val resource = "io.fabric8/webui/IDERunner.class"
      var url = IDERunner.getClass.getClassLoader.getResource(resource)
      if (url != null) {
        if (url.getProtocol == "jar") {
          // we are probably being run from a maven build..
          url = new java.net.URL(url.getFile.stripSuffix("!/" + resource))
          val jar = new File(url.getFile)
          if (jar.isFile) {
            return jar.getParentFile / (jar.getName.stripSuffix(".jar") + ".war")
          }
        } else if (url.getProtocol == "file") {
          // we are probably being run from an IDE...
          val rc = new File(url.getFile.stripSuffix("/" + resource))
          if (rc.isDirectory) {
            return rc / "src/main" / "src/main" / "target" / "webapp"
          }
        }
      }
      return null
    }

    def admin_app = {
      var app_context = new WebAppContext
      app_context.setContextPath(prefix)
      app_context.setWar(webapp.getCanonicalPath)
      app_context
    }

    val server = new Server
    server.setHandler(admin_app)
    server.setConnectors(Array[Connector](connector))
    server.start

    while (true) {
      Thread.sleep(1000)
    }
  }

}

