/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
@command(scope="apollo", name = "cluster-push", description = "Copies the local broker configuration to the cluster")
class ClusterPush extends Action {

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
      val data = (base / "etc" / "apollo.xml").read_text()

      val zk_client = new ZKClient(zookeeper, Timespan.parse("30s"), null)
      zk_client.start
      zk_client.waitForStart(Timespan.parse("30s"))
      zk_client.createOrSetWithParents(zk_root+"/config/"+conf, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
      zk_client.close

    } catch {
      case x:Helper.Failure=>
        System.err.println(x.getMessage)
      case x:Throwable=>
        x.printStackTrace
    }
    null
  }

}
