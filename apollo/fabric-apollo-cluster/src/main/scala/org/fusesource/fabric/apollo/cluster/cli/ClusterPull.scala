/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.cluster.cli


import org.apache.felix.gogo.commands.{Action, Option => option, Argument => argument, Command => command}
import org.apache.activemq.apollo.util.FileSupport._
import org.apache.felix.service.command.CommandSession
import org.apache.activemq.apollo.cli.commands.Helper
import org.linkedin.zookeeper.client.ZKClient
import org.linkedin.util.clock.Timespan
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.ZooDefs.Ids

/**
 * The apollo run command
 */
@command(scope="apollo", name = "cluster-pull", description = "Copies the cluster configuration to the local broker")
class ClusterPull extends Action {

  @option(name = "--zookeeper", description = "the zookeeper address")
  var zookeeper: String = "localhost:2181"

  @option(name = "--root", description = "the zookeeper root path.")
  var zk_root: String = "apollo"

  @option(name = "--conf", description = "the configuration name, 'default' if not set.")
  var conf: String = "default"

  def execute(session: CommandSession):AnyRef = {

    try {

      println(System.getProperty("apollo.base"))
      val base = system_dir("apollo.base")

      val zk_client = new ZKClient(zookeeper, Timespan.parse("30s"), null)
      zk_client.start
      zk_client.waitForStart(Timespan.parse("30s"))
      val data = zk_client.getData(zk_root+"/config/"+conf)

      (base / "etc" / "apollo.xml").write_bytes(data)


    } catch {
      case x:Helper.Failure=>
        System.err.println(x.getMessage)
      case x:Throwable=>
        x.printStackTrace
    }
    null
  }

}
