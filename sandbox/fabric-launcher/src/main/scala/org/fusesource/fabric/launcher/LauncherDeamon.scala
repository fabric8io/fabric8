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

package io.fabric8.launcher

import internal.{FileSupport, DefaultLaunchManager}
import internal.IOSupport._
import io.fabric8.launcher.api.{XmlCodec, JsonCodec, ServiceDTO, LaunchManager}
import java.net.URLClassLoader
import java.util.zip.ZipFile
import java.io._
import org.eclipse.jetty.server.{Connector, Server}
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.server.nio.SelectChannelConnector
import java.io.File
import java.lang.String
import java.net.URI
import web.RootResource

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object LauncherDeamon {

  def print_banner(out: PrintStream) = using(getClass().getResourceAsStream("banner.txt")) { source=>
    copy(source, out)
  }


  def main(args: Array[String]):Unit = {

    var conf_dir:File = null
    var stats_dir:File = null

    // parse the command line options..
    var remaining = args.toList
    while( !remaining.isEmpty ) {
      remaining match {
        case "--conf" :: value :: tail =>
          remaining = tail

          conf_dir = new File(value)
          if( !conf_dir.isDirectory ) {
            System.err.println("The conf setting '%s' is not a directory".format(value))
            System.exit(1)
          }


        case "--stats" :: value :: tail =>
          remaining = tail

          stats_dir = new File(value)
          if( !stats_dir.isDirectory ) {
            System.err.println("The stats setting '%s' is not a directory".format(value))
            System.exit(1)
          }


        case _ =>
          System.err.println("invalid arguments: "+remaining.mkString(" "))
          System.exit(1)
      }
    }

    if( conf_dir==null ) {
      System.err.println("The --conf option was not specified.")
      System.exit(1)
    }

    if( stats_dir==null ) {
      stats_dir = new File("stats")
    }


    print_banner(System.out)


    // Unpack the sigar native libs..
    unpack_native_libs

    // Load the launcher configurations..
    val launch_manager = new DefaultLaunchManager(stats_dir)

    RootResource.launch_manager = Some(launch_manager)

    import FileSupport._

    val bind ="http://127.0.0.1:8080"
    val bind_uri = new URI(bind)
    val prefix = "/"+bind_uri.getPath.stripPrefix("/")
    val host = bind_uri.getHost
    val port = bind_uri.getPort

    var connector = new SelectChannelConnector
    connector.setHost(host)
    connector.setPort(port)

    def webapp:File = {

      // the war might be on the classpath..
      val resource = "io/fabric8/launcher/banner.txt"
      var url = LauncherDeamon.getClass.getClassLoader.getResource(resource)
      if(url!=null) {
        if( url.getProtocol == "jar") {
          // we are probably being run from a maven build..
          url = new java.net.URL( url.getFile.stripSuffix("!/"+resource) )
          val jar = new File( url.getFile )
          if( jar.isFile ) {
            return jar.getParentFile / (jar.getName.stripSuffix(".jar")+".war")
          }
        } else if( url.getProtocol == "file") {
          // we are probably being run from an IDE...
          val rc = new File( url.getFile.stripSuffix("/"+resource) )
          if( rc.isDirectory ) {
            return rc/ "src/main" / "src/main" /"src"/"main"/"webapp"
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

    while(true) {
      launch_manager.configure(load(conf_dir))
      Thread.sleep(1000)
    }
  }

  def load(conf_dir:File):Seq[ServiceDTO] = {
    conf_dir.listFiles.flatMap { file=>
      if( file.isDirectory ) {
        None
      } else {
        try {
          // we support both xml and json formats..
          if( file.getName.endsWith(".json") ) {
            using( new FileInputStream(file)) { is =>
                Some(JsonCodec.decode(classOf[ServiceDTO], is, System.getProperties))
            }
          } else if( file.getName.endsWith(".xml") ) {
            using( new FileInputStream(file)) { is =>
                Some(XmlCodec.decode(classOf[ServiceDTO], is, System.getProperties))
            }
          } else {
            None
          }
        } catch {
          case e:Exception =>
            println("Invalid launcher configuration file '%s'. Error: %s".format(file, e.getMessage))
            None
        }
      }
    }
  }

  /**
   * Sigar expects the native libs to be in the same directory as the sigar jar file.
   */
  def unpack_native_libs: Unit = {
    getClass.getClassLoader match {
      case x: URLClassLoader =>
        x.getURLs.foreach {
          url =>
            val fn = url.getFile
            val file = new File(fn)
            if (fn.matches(""".*sigar-[^-]+-native.jar""") && file.exists) {
              val zip = new ZipFile(file)

              val entries = zip.entries
              while (entries.hasMoreElements) {
                val entry = entries.nextElement
                if (entry.getName.matches(""".*\.dll|.*\.so|.*\.dylib|.*\.sl|.*\.nlm""")) {
                  val target = new File(file.getParentFile, entry.getName)
                  if (!target.exists || target.length != entry.getSize) {
                    try {
                      using(new FileOutputStream(target)) {
                        os =>
                          using(zip.getInputStream(entry)) {
                            is =>
                              copy(is, os)
                          }
                      }
                      try {
                        target.setExecutable(true)
                      } catch {
                        case _ => // setExecutable is a java 1.6 method.. ignore if it's not available.
                      }
                    }
                    catch {
                      case e:Throwable => // We probably don't have write access.. ignore.
                    }
                  }
                }
              }
              zip.close
            }
        }
    }
  }
}

