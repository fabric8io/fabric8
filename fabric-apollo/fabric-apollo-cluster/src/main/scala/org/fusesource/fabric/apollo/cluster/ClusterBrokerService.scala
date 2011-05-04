/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.cluster

import org.apache.activemq.apollo.broker.Broker
import org.osgi.service.cm.ConfigurationAdmin
import org.osgi.framework._
import java.lang.String
import collection.JavaConversions._
import org.apache.activemq.apollo.util._
import FileSupport._
import java.io.File
import org.linkedin.zookeeper.client.IZKClient
import org.fusesource.hawtbuf.ByteArrayInputStream
import org.fusesource.fabric.apollo.cluster.dto.ClusterRouterDTO
import org.fusesource.hawtdispatch._
import org.apache.zookeeper.AsyncCallback.DataCallback
import org.apache.zookeeper.data.Stat
import org.apache.zookeeper.{WatchedEvent, Watcher}
import org.apache.zookeeper.Watcher.Event.EventType
import java.util.concurrent.TimeUnit
import java.util.{Arrays, Properties}
import org.apache.activemq.apollo.dto.{BrokerDTO, XmlCodec}
import org.apache.activemq.apollo.broker.osgi.BrokerService

object ClusterBrokerService extends Log {

}

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class ClusterBrokerService extends Dispatched {
  import ClusterBrokerService._

  var id:String = _
  var basedir: File = _
  var broker:ClusterBroker = _
  var zk_client:IZKClient = _
  var zk_root:String = _
  var config_name:String = "default"

  var started_counter = 0

  var cluster:ZkCluster = _

  var started = false

  var config_data:Array[Byte] = _

  def dispatch_queue: DispatchQueue = createQueue()

  def fetch_config:Unit = dispatch_queue {
    object callback extends DataCallback with Watcher {
      def process(event: WatchedEvent): Unit = {
        event.getType match {
          case EventType.NodeCreated=> fetch_config
          case EventType.NodeDataChanged=> fetch_config
          case EventType.NodeChildrenChanged => fetch_config
          case _ =>
            info("Cluster Config Fetch Failed: "+event)
            // try again in 5 seconds..
            dispatch_queue.after(5, TimeUnit.SECONDS) {
              fetch_config
            }
        }
      }
      def processResult(rc: Int, path: String, ctx: AnyRef, data: Array[Byte], stat: Stat): Unit = dispatch_queue {
        if( stat==null ) {
          warn("Cluster configuration missing from ZooKeeper path '%s', perhaps you forgot to use the 'cluster-push' command.".format(path))
          dispatch_queue.after(5, TimeUnit.SECONDS) {
            fetch_config
          }
        }
        if( !Arrays.equals(config_data, data) ) {
          config_data = data
          on_config_load
        }
      }
    }
    debug("Fetching cluster configuration")
    zk_client.getData(zk_root+"/config/"+config_name, callback, callback, null)
  }

  def start(): Unit = (dispatch_queue !! {
    if( !started ) {
      started = true
      fetch_config
    }
  }).await

  def stop(): Unit = (dispatch_queue !! {
    if( started ) {
      started = false
      if( cluster != null ) {
        cluster.leave
      }
      if( broker!=null ) {
        ServiceControl.stop(broker, "stopping broker")
        broker = null
      }
    }
  }).await(2, TimeUnit.SECONDS)

  private def on_config_load = dispatch_queue {
    if( started ) {

      def start_broker: Unit = if( config_data!=null ) {
        if( cluster==null ) {
          cluster = new ZkCluster(zk_client, zk_root+"/online")
          cluster.start
        }

        val props: Properties = config_props
        val data = config_data
        val config = XmlCodec.decode(classOf[BrokerDTO], new ByteArrayInputStream(data), props)
        config.virtual_hosts.foreach {host =>
          host.router = new ClusterRouterDTO
        }

        debug("Starting broker");
        broker = new ClusterBroker(id, cluster)
        broker.configure(config, LoggingReporter(ClusterBrokerService))
        broker.tmp = basedir / "tmp"
        broker.tmp.mkdirs
        broker.start(dispatch_queue.runnable {
          started_counter += 1
          info("Apollo started");
        })
      }

      // we need to shutdown first...
      if( broker!=null ) {
        info("Broker configuration updated.  Restarting...");
        broker.stop(dispatch_queue.runnable {
          broker = null
          start_broker
        })
      } else {
        start_broker
      }
    }
  }

  protected def config_props: Properties = {
    val props = new Properties
    props.putAll(System.getProperties)
    props.put("apollo.base", basedir.getCanonicalPath)
    props.put("id", id)
    props
  }

}


/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object OsgiClusterBrokerService extends ClusterBrokerService {

  var context: BundleContext = _
  var configAdmin:ConfigurationAdmin = _

  override protected def config_props: Properties = {
    val props = super.config_props
    val cmProps = configAdmin.getConfiguration("org.apache.activemq.apollo").getProperties
    if (cmProps != null) {
      cmProps.keySet.foreach {key =>
        props.put(key.asInstanceOf[String], cmProps.get(key).asInstanceOf[String])
      }
    }
    props
  }

}


/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class OsgiClusterBrokerService {
  import OsgiClusterBrokerService._

  //
  // Setters to allow blueprint injection.
  //
  def setContext(value:BundleContext):Unit = context = value
  def setBasedir(value:File):Unit = basedir = value

  def setConfigAdmin(value:ConfigurationAdmin):Unit = configAdmin = value
  def setZooKeeper(value:IZKClient):Unit = zk_client = value
  def setZkBase(value:String):Unit = zk_root = value
  def setId(value:String):Unit = id = value

  def start() = OsgiClusterBrokerService.start
  def stop() = OsgiClusterBrokerService.stop
}

