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

package org.fusesource.mq.fabric

import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedServiceFactory
import org.slf4j.LoggerFactory
import reflect.BeanProperty
import java.util.{Properties, Dictionary}
import collection.mutable.HashMap
import java.util.concurrent.atomic.AtomicBoolean
import org.springframework.core.io.Resource
import org.apache.activemq.spring.Utils
import org.apache.xbean.spring.context.ResourceXmlApplicationContext
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader
import java.beans.PropertyEditorManager
import java.net.URI
import org.apache.xbean.spring.context.impl.URIEditor
import org.springframework.beans.factory.FactoryBean
import org.apache.activemq.util.IntrospectionSupport
import org.apache.activemq.broker.{TransportConnector, BrokerService}
import scala.collection.JavaConversions._
import java.lang.{ThreadLocal, Thread}
import org.apache.activemq.ActiveMQConnectionFactory
import org.osgi.framework.{ServiceRegistration, BundleContext}
import org.apache.activemq.network.DiscoveryNetworkConnector
import java.util
import collection.mutable
import org.apache.curator.framework.CuratorFramework
import org.fusesource.mq.fabric.FabricDiscoveryAgent.ActiveMQNode
import org.fusesource.fabric.groups.{Group, GroupListener}
import GroupListener.GroupEvent
import org.fusesource.fabric.api.FabricService

object ActiveMQServiceFactory {
  final val LOG= LoggerFactory.getLogger(classOf[ActiveMQServiceFactory])
  final val CONFIG_PROPERTIES = new ThreadLocal[Properties]()

  PropertyEditorManager.registerEditor(classOf[URI], classOf[URIEditor])

  def info(str: String, args: AnyRef*) = if (LOG.isInfoEnabled) {
    LOG.info(String.format(str, args:_*))
  }

  def debug(str: String, args: AnyRef*) = if (LOG.isDebugEnabled) {
    LOG.debug(String.format(str, args:_*))
  }

  def warn(str: String, args: AnyRef*) = if (LOG.isWarnEnabled) {
    LOG.warn(String.format(str, args:_*))
  }

  implicit def toProperties(properties: Dictionary[_, _]) = {
    var props: Properties = new Properties
    var ek = properties.keys
    while (ek.hasMoreElements) {
      var key = ek.nextElement
      var value = properties.get(key)
      props.put(key.toString, if (value != null) value.toString else "")
    }
    props
  }

  def arg_error[T](msg:String):T = {
    throw new IllegalArgumentException(msg)
  }

  def createBroker(uri: String, properties:Properties) = {
    CONFIG_PROPERTIES.set(properties)
    try {
      Thread.currentThread.setContextClassLoader(classOf[BrokerService].getClassLoader)
      var resource: Resource = Utils.resourceFromString(uri)
      val ctx = new ResourceXmlApplicationContext((resource)) {
        protected override def initBeanDefinitionReader(reader: XmlBeanDefinitionReader): Unit = {
          reader.setValidating(false)
        }
      }
      var names: Array[String] = ctx.getBeanNamesForType(classOf[BrokerService])
      val broker = names.flatMap{ name=> Option(ctx.getBean(name).asInstanceOf[BrokerService]) }.headOption.getOrElse(arg_error("Configuration did not contain a BrokerService"))
      val networks = Option(properties.getProperty("network")).getOrElse("").split(",")
      networks.foreach {name =>
        if (!name.isEmpty) {
          LOG.info("Adding network connector " + name)
          val nc = new DiscoveryNetworkConnector(new URI("fabric:" + name))
          nc.setName("fabric-" + name)
          // copy properties as IntrospectionSupport removes them
          val network_properties = new mutable.HashMap[String, Object]()
          network_properties.putAll(properties.asInstanceOf[java.util.Map[String, String]])
          IntrospectionSupport.setProperties(nc, network_properties, "network.")
          broker.addNetworkConnector(nc)
        }
      }
      (ctx, broker, resource)
    } finally {
      CONFIG_PROPERTIES.remove()
    }
  }

}

class ConfigurationProperties extends FactoryBean[Properties] {
  def getObject = new Properties(ActiveMQServiceFactory.CONFIG_PROPERTIES.get())
  def getObjectType = classOf[Properties]
  def isSingleton = false
}

class ActiveMQServiceFactory extends ManagedServiceFactory {

  import ActiveMQServiceFactory._

  @BeanProperty
  var bundleContext: BundleContext = null
  @BeanProperty
  var curator: CuratorFramework = null

  var owned_pools = Set[String]()

  @volatile
  var fabricService:FabricService = _

  def bindFabricService(fabricService:FabricService) = {
    this.fabricService = fabricService
  }

  def unbindFabricService(fabricService:FabricService) = {
    this.fabricService = null;
  }
  
  def can_own_pool(cc:ClusteredConfiguration) = this.synchronized {
    if( cc.pool==null )
      true
    else
      !owned_pools.contains(cc.pool)
  }

  def take_pool(cc:ClusteredConfiguration) = this.synchronized {
    if( cc.pool==null ) {
      true
    } else {
      if( owned_pools.contains(cc.pool) ) {
        false
      } else {
        owned_pools += cc.pool
        fire_pool_change(cc)
        true
      }
    }
  }

  def return_pool(cc:ClusteredConfiguration) = this.synchronized {
    if( cc.pool!=null ) {
      owned_pools -= cc.pool
      fire_pool_change(cc)
    }
  }
  
  def fire_pool_change(cc:ClusteredConfiguration) = {
    new Thread(){
      override def run() {
        ActiveMQServiceFactory.this.synchronized {
          configurations.values.foreach { c=>
            if ( c!=cc && c.pool == cc.pool ) {
              c.update_pool_state
            }
          }
        }
      }
    }.start
  }
  

  case class ClusteredConfiguration(properties:Properties) {

    val name = Option(properties.getProperty("broker-name")).getOrElse(System.getProperty("karaf.name"))
    val data = Option(properties.getProperty("data")).getOrElse("data" + System.getProperty("file.separator") + name)
    val config = Option(properties.getProperty("config")).getOrElse(arg_error("config property must be set"))
    val group = Option(properties.getProperty("group")).getOrElse("default")
    val pool = Option(properties.getProperty("standby.pool")).getOrElse("default")
    val connectors = Option(properties.getProperty("connectors")).getOrElse("").split("""\s""")
    val replicating:Boolean = "true".equalsIgnoreCase(Option(properties.getProperty("replicating")).getOrElse("false"))
    val standalone:Boolean = "true".equalsIgnoreCase(Option(properties.getProperty("standalone")).getOrElse("false"))
    val registerService:Boolean = "true".equalsIgnoreCase(Option(properties.getProperty("registerService")).getOrElse("true"))

    val started = new AtomicBoolean

    var pool_enabled = false
    def update_pool_state = this.synchronized {
      val value = can_own_pool(this)
      if( pool_enabled != value) {
        pool_enabled = value
        if( value ) {
          if( pool!=null ) {
            info("Broker %s added to pool %s.", name, pool)
          }
          discoveryAgent.start()
        } else {
          if( pool!=null ) {
            info("Broker %s removed from pool %s.", name, pool)
          }
          discoveryAgent.stop()
        }
      }
    }
    var discoveryAgent:FabricDiscoveryAgent = null
    @volatile
    var start_thread:Thread = _
    @volatile
    var server:(ResourceXmlApplicationContext, BrokerService, Resource) = _

    var cfServiceRegistration:ServiceRegistration[_] = null

    var last_modified:Long = -1


    def ensure_broker_name_is_set = {
      if (!properties.containsKey("broker-name")) {
        properties.setProperty("broker-name", name)
      }
      if (!properties.containsKey("data")) {
        properties.setProperty("data", data)
      }
    }

    ensure_broker_name_is_set

    if (standalone) {
      if (started.compareAndSet(false, true)) {
        info("Standalone broker %s is starting.", name)
        start
      }
    } else if (replicating) {
      if (started.compareAndSet(false, true)) {
        info("Replicating broker %s is starting.", name)
        start
      }
    } else {
      info("Broker %s is waiting to become the master", name)

      discoveryAgent = new FabricDiscoveryAgent
      discoveryAgent.setAgent(System.getProperty("karaf.name"))
      discoveryAgent.setId(name)
      discoveryAgent.setGroupName(group)
      discoveryAgent.setCurator(curator)
      discoveryAgent.getGroup.add(new GroupListener[ActiveMQNode]() {
        def groupEvent(group: Group[ActiveMQNode], event: GroupEvent) {
          if (discoveryAgent.getGroup.isMaster) {
            if (started.compareAndSet(false, true)) {
              if (take_pool(ClusteredConfiguration.this)) {
                info("Broker %s is now the master, starting the broker.", name)
                start
              } else {
                update_pool_state
                started.set(false)
              }
            }
          } else {
            if (started.compareAndSet(true, false)) {
              return_pool(ClusteredConfiguration.this)
              info("Broker %s is now a slave, stopping the broker.", name)
              stop()
            }
          }
        }
      })
      update_pool_state
    }

    def close = this.synchronized {
      if(  pool_enabled || (replicating && discoveryAgent!=null)  ) {
        discoveryAgent.stop()
      }
      if( pool_enabled ) {
        return_pool(ClusteredConfiguration.this)
      }
      if(started.compareAndSet(true, false)) {
        stop(false)
      }
    }

    def osgiRegister(broker: BrokerService): Unit = {
      val connectionFactory = new ActiveMQConnectionFactory("vm://" + broker.getBrokerName + "?create=false")
      cfServiceRegistration = bundleContext.registerService(classOf[javax.jms.ConnectionFactory].getName, connectionFactory, HashMap("name" -> broker.getBrokerName))
      debug("registerService of type " + classOf[javax.jms.ConnectionFactory].getName  + " as: " + connectionFactory + " with name: " + broker.getBrokerName + "; " + cfServiceRegistration)
    }

    def osgiUnregister(broker: BrokerService): Unit = {
      if (cfServiceRegistration != null) cfServiceRegistration.unregister()
      debug("unregister connection factory for: " + broker.getBrokerName + "; " + cfServiceRegistration)
    }

    def start = {
      // Startup async so that we do not block the ZK event thread.
      def trystartup:Unit = {
        start_thread = new Thread("Startup for ActiveMQ Broker: " + name) {

          def configure_ports(service: BrokerService, properties: Properties) = {
            service.getTransportConnectors.foreach {
              t => {
                val portKey = t.getName() + "-port"
                if (properties.containsKey(portKey)) {
                  val template = t.getUri;
                  t.setUri(new URI(template.getScheme, template.getUserInfo, template.getHost,
                    Integer.valueOf("" + properties.get(portKey)),
                    template.getPath, template.getQuery, template.getFragment))
                }
              }
            }
          }

          override def run() {
            var start_failure:Throwable = null
            try {

              // If we are in a fabric, let pass along the zk password in the props.
              val fs = fabricService
              if( fs != null ) {
                val container = fs.getCurrentContainer
                if( !properties.containsKey("container.id") ) {
                  properties.setProperty("container.id", container.getId)
                }
                if( !properties.containsKey("container.ip") ) {
                  properties.setProperty("container.ip", container.getIp)
                }
                if( !properties.containsKey("zookeeper.url") ) {
                  properties.setProperty("zookeeper.url", fs.getZookeeperUrl)
                }
                if( !properties.containsKey("zookeeper.password") ) {
                  properties.setProperty("zookeeper.password", fs.getZookeeperPassword)
                }
              }

              // ok boot up the server..
              server = createBroker(config, properties)
              server._2.addShutdownHook(new Runnable(){
                def run:Unit = {
                  // Start up the server again if it shutdown.  Perhaps
                  // it has lost a Locker and wants a restart.
                  if(started.get){
                    trystartup
                  }
                }
              })
              configure_ports(server._2, properties)
              server._2.start()
              info("Broker %s has started.", name)

              if( replicating ) {
                discoveryAgent = new FabricDiscoveryAgent
                discoveryAgent.setAgent(System.getProperty("karaf.name"))
                discoveryAgent.setId(name)
                discoveryAgent.setGroupName(group)
                discoveryAgent.setCurator(curator)
                discoveryAgent.start()
              }

              // Update the advertised endpoint URIs that clients can use.
              if (!standalone || replicating) {
                discoveryAgent.setServices( connectors.flatMap { name=>
                  val connector = server._2.getConnectorByName(name)
                  if ( connector==null ) {
                    warn("ActiveMQ broker '%s' does not have a connector called '%s'", name, name)
                    None
                  } else {
                    Some(connector.getConnectUri.getScheme + "://${zk:" + System.getProperty("karaf.name") + "/ip}:" + connector.getConnectUri.getPort)
                  }
                })
              }

              if (registerService) osgiRegister(server._2)
            } catch {
              case e:Throwable =>
                info("Broker %s failed to start.  Will try again in 10 seconds", name)
                LOG.info("Exception on start: " + e, e)
                Thread.sleep(1000*10);
                start_failure = e
            } finally {
              if(started.get && start_failure!=null && !isInterrupted){
                trystartup
              } else {
                start_thread = null
                last_modified = server._3.lastModified()
              }
            }
          }
        }
        start_thread.start()
      }
      trystartup
    }

    val stop_runnable = new Runnable {
      override def run() {
        val s = server // working with a volatile
        if( s!=null ) {
          try {
            s._2.stop()
            s._2.waitUntilStopped()
            if (registerService) {
              osgiUnregister(s._2)
            }
          } catch {
            case e:Throwable => LOG.debug("Exception on stop: " + e,  e)
          }
          try {
            s._1.close()
          } catch {
            case e:Throwable => LOG.debug("Exception on close: " + e,  e)
          }
          try {
            if ( pool_enabled || (replicating && discoveryAgent!=null) ) {
              discoveryAgent.stop()
            }
          } catch {
            case e:Throwable => LOG.debug("Exception on stop: " + e,  e)
          }
          server = null
        }

        var t = start_thread // working with a volatile
          while(t!=null) {
            t.interrupt()
            t.join()
            t = start_thread // when the start up thread gives up trying to start this gets set to null.
          }
      }
    }
    
    def stop(async: Boolean = true) = {
      info("Broker %s is being stopped.", name)
      if (async) {
        new Thread(stop_runnable, "Stop for ActiveMQ Broker: "+name).start
      } else {
        stop_runnable.run()
      }
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  // Maintain a registry of configuration based on ManagedServiceFactory events.
  ////////////////////////////////////////////////////////////////////////////
  val configurations = new HashMap[String, ClusteredConfiguration]

  class ConfigThread extends Thread {

    var running = true

    override def run() {
      while (running) {
        configurations.values.foreach(c => {
          if (c.last_modified != -1 && c.server._3.lastModified() != c.last_modified) {
            c.last_modified = c.server._3.lastModified()
            info("updating " + c.properties)
            updated(c.properties.get("service.pid").asInstanceOf[String], c.properties.asInstanceOf[Dictionary[java.lang.String, _]])
          }
        })
        try {
          Thread.sleep(5 * 1000)
        } catch {
          case e : InterruptedException => {}
        }
      }
    }
  }

  val config_thread = new ConfigThread()
  config_thread.setName("ActiveMQ Configuration Watcher")
  config_thread.start()

  def updated(pid: String, properties: Dictionary[java.lang.String, _]): Unit = this.synchronized {
    try {
      deleted(pid)
      configurations.put(pid, ClusteredConfiguration(properties))
    } catch {
      case e: Exception => throw new ConfigurationException(null, "Unable to parse ActiveMQ configuration: " + e.getMessage).initCause(e).asInstanceOf[ConfigurationException]
    }
  }
  def deleted(pid: String): Unit = this.synchronized {
    configurations.remove(pid).foreach(_.close)
  }

  def destroy: Unit = this.synchronized {
    configurations.keys.toArray.foreach(deleted(_))
    config_thread.running = false
    config_thread.interrupt()
  }

  def getName: String = {
    return "ActiveMQ Server Controller"
  }

}