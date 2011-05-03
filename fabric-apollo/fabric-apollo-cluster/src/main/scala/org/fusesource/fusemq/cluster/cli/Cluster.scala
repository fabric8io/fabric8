/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fusemq.cluster.cli


import org.apache.felix.gogo.commands.{Action, Option => option, Argument => argument, Command => command}
import java.io.File
import org.apache.activemq.apollo.util.FileSupport._
import org.apache.activemq.apollo.cli.Apollo
import org.apache.felix.service.command.CommandSession
import org.apache.activemq.apollo.cli.commands.Helper
import org.linkedin.zookeeper.client.ZKClient
import org.linkedin.util.clock.Timespan
import org.fusesource.fusemq.cluster.ClusterBrokerService
import org.apache.activemq.apollo.util.ServiceControl

/**
 * The apollo run command
 */
@command(scope="apollo", name = "cluster", description = "runs a clustered broker instance")
class Cluster extends Action {

  @option(name = "--tmp", description = "A temp directory.")
  var tmp: File = _

  @option(name = "--zookeeper", description = "the zookeeper address")
  var zookeeper: String = "localhost:2181"

  @option(name = "--root", description = "the zookeeper root path.")
  var zk_root: String = "apollo"

  @option(name = "--conf", description = "the configuration name, 'default' if not set.")
  var conf: String = "default"

  @argument(name = "id", description = "the id of this cluster node.", index=0, required=true)
  var id: String = _

  var broker_service:ClusterBrokerService = _

  def execute(session: CommandSession):AnyRef = {

    try {

      val base = system_dir("apollo.base")

      // todo.. perhaps put this in ZK too.
      if( System.getProperty("java.security.auth.login.config")==null ) {
        val login_config = base / "etc" / "login.config"
        if( login_config.exists ) {
          System.setProperty("java.security.auth.login.config", login_config.getCanonicalPath)
        }
      }

      if( tmp == null ) {
        tmp = base / "tmp"
        tmp.mkdirs
      }

      // Set the config as system propeties so that they can be used via variable subst
      // in the configuration file.
      System.setProperty("apollo.cluster.id", id)
      System.setProperty("apollo.cluster.conf", conf)
      System.setProperty("apollo.cluster.zk", zookeeper)
      System.setProperty("apollo.cluster.zk.root", zk_root)

      Apollo.print_banner(session.getConsole)


      val zk_client = new ZKClient(zookeeper, Timespan.parse("30s"), null)
      zk_client.start
      zk_client.waitForStart(Timespan.parse("30s"))

      broker_service = new ClusterBrokerService
      broker_service.basedir = base
      broker_service.zk_client = zk_client
      broker_service.zk_root = zk_root
      broker_service.id = id
      broker_service.config_name = conf
      broker_service.start

      Runtime.getRuntime.addShutdownHook(new Thread(){
        override def run: Unit = {
          broker_service.stop
        }
      })

      // wait forever...  broker will system exit.
      this.synchronized {
        this.wait
      }

    } catch {
      case x:Helper.Failure=>
        System.err.println(x.getMessage)
      case x:Throwable=>
        x.printStackTrace
    }
    null
  }

}
