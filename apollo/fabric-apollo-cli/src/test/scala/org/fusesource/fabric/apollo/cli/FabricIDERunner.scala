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

package org.fusesource.fabric.apollo.cli

import org.junit.Test
import java.io.File
import org.apache.activemq.apollo.util.FileSupport._
import org.apache.activemq.apollo.cli.Apollo
import org.apache.activemq.apollo.web.resources.BrokerResource

/**
 * <p>
 * Launches the Apollo broker assuming it's being run from
 * an IDE environment.
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object FabricIDERunner  {
  def main(args:Array[String]) = {

    // Let the user know where he configure logging at.
    println("Logging was configured using '%s'.".format(getClass.getClassLoader.getResource("log4j.properties")));

    // Setups where the broker base directory is...
    if( System.getProperty("apollo.base") == null ) {
      val apollo_base = new File(getClass.getClassLoader.getResource("example-fabric/etc/apollo.xml").toURI.resolve("..").toURL.getFile)
      System.setProperty("apollo.base", apollo_base.getCanonicalPath)
    }
    println("apollo.base=%s".format(System.getProperty("apollo.base")));

    // Setup where the web app resources are...
    if( System.getProperty("apollo.webapp") == null ) {
      val web_project_root = new File(classOf[BrokerResource].getProtectionDomain.getCodeSource.getLocation.toURI.resolve("../..").toURL.getFile)
      val apollo_webapp = web_project_root / "src" / "main"/ "webapp"
      System.setProperty("apollo.webapp", apollo_webapp.getCanonicalPath)
    }

    // Configure jul logging..
    val apollo_base = new File(System.getProperty("apollo.base"))
    val jul_properties = apollo_base / "etc" / "jul.properties"
    System.setProperty("java.util.logging.config.file", jul_properties.getCanonicalPath)

    println("=======================")
    println("Press Enter To Shutdown")
    println("=======================")

    new Apollo().run(if(args.isEmpty) Array("run") else args)
    System.in.read
    println("=============")
    println("Shutting down")
    println("=============")
    System.exit(0)
  }
}
