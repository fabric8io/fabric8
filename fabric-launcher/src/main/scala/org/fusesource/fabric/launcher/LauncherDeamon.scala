/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.launcher

import org.fusesource.hawtdispatch._
import java.util.concurrent.TimeUnit
import internal.FileSupport._
import internal.IOSupport._
import org.fusesource.fabric.launcher.internal.DefaultLaunchManager
import org.fusesource.fabric.launcher.api.{XmlCodec, JsonCodec, ServiceDTO, LaunchManager}
import org.hyperic.jni.ArchLoader
import java.net.URLClassLoader
import java.util.zip.ZipFile
import java.io._

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
    val launch_manager:LaunchManager = new DefaultLaunchManager(stats_dir)

    while(true) {
      launch_manager.configure(load(conf_dir))
      val values = launch_manager.status()
      if( !values.isEmpty ) {
        println("========= sevice status ==========")
        values.foreach { status =>
          println(new String(JsonCodec.encode(status), "UTF-8"))
        }
      }
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

