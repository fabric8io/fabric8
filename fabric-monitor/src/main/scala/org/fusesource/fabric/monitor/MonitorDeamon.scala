/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.monitor


import api.{Monitor, PollerFactory, MonitoredSetDTO}
import internal.IOSupport._
import internal.{ClassFinder, DefaultMonitor}
import java.net.URLClassLoader
import java.util.zip.ZipFile
import java.io._
import org.fusesource.fabric.monitor.api.{XmlCodec, JsonCodec}
import collection.mutable.HashMap

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object MonitorDeamon {

  val DATA_POLLER_FACTORY_RESOURCE = "META-INF/services/org.fusesource.fabric.monitor/poller-factory.index"


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


    val finder = new ClassFinder(DATA_POLLER_FACTORY_RESOURCE, classOf[PollerFactory])


    // Load the launcher configurations..
    val monitor:Monitor = new DefaultMonitor("")
    monitor.poller_factories = finder.singletons

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
                Some(JsonCodec.decode(classOf[MonitoredSetDTO], is, System.getProperties))
            }
          } else if( file.getName.endsWith(".xml") ) {
            using( new FileInputStream(file)) { is =>
                Some(XmlCodec.decode(classOf[MonitoredSetDTO], is, System.getProperties))
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

