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

package io.fabric8.monitor


import internal.IOSupport._
import internal.{ClassFinder, DefaultMonitor}
import java.net.URLClassLoader
import java.util.zip.ZipFile
import java.io._
import collection.mutable.HashMap
import io.fabric8.monitor.api._
import scala.Some

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object MonitorDeamon {

  private val DATA_POLLER_FACTORY_RESOURCE = "META-INF/services/io.fabric8.monitor/poller-factory.index"
  private val finder = new ClassFinder(DATA_POLLER_FACTORY_RESOURCE, classOf[PollerFactory])

  def poller_factories = finder.singletons

  def main(args: Array[String]):Unit = {

    var conf:String = null

    // parse the command line options..
    var remaining = args.toList
    while( !remaining.isEmpty ) {
      remaining match {
        case "--conf" :: value :: tail =>
          conf = value
          remaining = tail

        case _ =>
          System.err.println("invalid arguments: "+remaining.mkString(" "))
          System.exit(1)
      }
    }

    if( conf==null ) {
      System.err.println("The --conf option was not specified.")
      System.exit(1)
  }

    val conf_dir = new File(conf)
    if( !conf_dir.isDirectory ) {
      System.err.println("The conf setting '%s' is not a directory".format(conf))
      System.exit(1)
    }


    // Unpack the sigar native libs..
    unpack_native_libs


    // Load the launcher configurations..
    val monitor:Monitor = new DefaultMonitor("")
    monitor.poller_factories = poller_factories

    while(true) {
      monitor.configure(load(conf_dir))
      Thread.sleep(1000)
    }
  }

  def load(conf_dir:File):Seq[MonitoredSetDTO] = {
    conf_dir.listFiles.flatMap { file=>
      if( file.isDirectory ) {
        None
      } else {
        try {
          // we support both xml and json formats..
          if( file.getName.endsWith(".json") ) {
            using( new FileInputStream(file)) { is =>
                //Some(JsonCodec.decode(classOf[MonitoredSetDTO], is, System.getProperties))
                Some(JsonCodec.decode(classOf[MonitoredSetDTO], is))
            }
          } else if( file.getName.endsWith(".xml") ) {
            using( new FileInputStream(file)) { is =>
                //Some(XmlCodec.decode(classOf[MonitoredSetDTO], is, System.getProperties))
                Some(XmlCodec.decode(classOf[MonitoredSetDTO], is))
            }
          } else {
            None
          }
        } catch {
          case e:Exception =>
            e.printStackTrace
            println("Invalid monitor configuration file '%s'. Error: %s".format(file, e.getMessage))
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

