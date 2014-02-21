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
import java.util.concurrent.atomic.{AtomicInteger, AtomicBoolean}
import org.springframework.core.io.Resource
import org.apache.activemq.spring.Utils
import org.apache.xbean.spring.context.ResourceXmlApplicationContext
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader
import java.beans.PropertyEditorManager
import java.net.{URL, URI}
import org.apache.xbean.spring.context.impl.URIEditor
import org.springframework.beans.factory.FactoryBean
import org.apache.activemq.util.IntrospectionSupport
import org.apache.activemq.broker.BrokerService
import scala.collection.JavaConversions._
import java.lang.{ThreadLocal, Thread}
import org.apache.activemq.ActiveMQConnectionFactory
import org.apache.activemq.spring.SpringBrokerContext
import org.osgi.framework.{FrameworkUtil, ServiceReference, ServiceRegistration, BundleContext}
import org.apache.activemq.network.DiscoveryNetworkConnector
import collection.mutable
import org.apache.curator.framework.CuratorFramework
import org.fusesource.mq.fabric.FabricDiscoveryAgent.ActiveMQNode
import io.fabric8.groups.{Group, GroupListener}
import GroupListener.GroupEvent
import io.fabric8.api.FabricService
import org.apache.xbean.classloader.MultiParentClassLoader
import org.osgi.util.tracker.{ServiceTrackerCustomizer, ServiceTracker}
import org.osgi.service.url.URLStreamHandlerService
import java.util.concurrent.{Future, Executors}

object ActiveMQServiceFactory {
  final val LOG= LoggerFactory.getLogger(classOf[ActiveMQServiceFactory])
  final val CONFIG_PROPERTIES = new ThreadLocal[Properties]()

  PropertyEditorManager.registerEditor(classOf[URI], classOf[URIEditor])

  def info(str: String) = if (LOG.isInfoEnabled) {
    LOG.info(str)
  }

  def info(str: String, args: AnyRef*) = if (LOG.isInfoEnabled) {
    LOG.info(String.format(str, args:_*))
  }

  def debug(str: String) = if (LOG.isDebugEnabled) {
    LOG.debug(str)
  }

  def debug(str: String, args: AnyRef*) = if (LOG.isDebugEnabled) {
    LOG.debug(String.format(str, args:_*))
  }

  def warn(str: String) = if (LOG.isWarnEnabled) {
    LOG.warn(str)
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
      val classLoader = new MultiParentClassLoader("xbean", Array[URL](), Array[ClassLoader](this.getClass.getClassLoader, classOf[BrokerService].getClassLoader))
      Thread.currentThread().setContextClassLoader(classLoader)
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

          val network_properties = new mutable.HashMap[String, Object]()
          //use default credentials for network connector (if none was specified)
          network_properties.put("network.userName", "admin")
          network_properties.put("network.password", properties.getProperty("zookeeper.password"))
          network_properties.putAll(properties.asInstanceOf[java.util.Map[String, String]])
          IntrospectionSupport.setProperties(nc, network_properties, "network.")
          broker.addNetworkConnector(nc)
        }
      }
      var brokerContext = new SpringBrokerContext
      brokerContext.setConfigurationUrl(resource.getURL.toExternalForm)
      brokerContext.setApplicationContext(ctx)
      broker.setBrokerContext(brokerContext)
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

class ActiveMQServiceFactory(bundleContext: BundleContext) extends ManagedServiceFactory
  with ServiceTrackerCustomizer[CuratorFramework,CuratorFramework] {

  import ActiveMQServiceFactory._


  //
  // Pool management
  //

  var owned_pools = Set[String]()

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
              c.update_pool_state()
            }
          }
        }
      }
    }.start()
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
    val config_check = "true".equalsIgnoreCase(Option(properties.getProperty("config.check")).getOrElse("true"))


    val started = new AtomicBoolean
    val startAttempt = new AtomicInteger

    var pool_enabled = false
    def update_pool_state() = this.synchronized {
      val value = can_own_pool(this)
      if( pool_enabled != value ) {
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
    val executor = Executors.newSingleThreadExecutor()
    var start_future: Future[_] = null
    var stop_future: Future[_] = null
    @volatile
    var server:(ResourceXmlApplicationContext, BrokerService, Resource) = _

    var cfServiceRegistration:ServiceRegistration[_] = null

    var last_modified:Long = -1

    def updateCurator(curator: CuratorFramework) = {
      if (!standalone) {
        this.synchronized {
          if (discoveryAgent != null) {
            discoveryAgent.stop()
            discoveryAgent = null
            if (started.compareAndSet(true, false)) {
              info("Lost zookeeper service for broker %s, stopping the broker.", name)
              stop()
              waitForStop()
              return_pool(this)
              pool_enabled = false
            }
          }
          waitForStop()
          if (curator != null) {
            info("Found zookeeper service for broker %s.", name)
            discoveryAgent = new FabricDiscoveryAgent
            discoveryAgent.setAgent(System.getProperty("karaf.name"))
            discoveryAgent.setId(name)
            discoveryAgent.setGroupName(group)
            discoveryAgent.setCurator(curator)
            if (replicating) {
              discoveryAgent.start()
              if (started.compareAndSet(false, true)) {
                info("Replicating broker %s is starting.", name)
                start()
              }
            } else {
              discoveryAgent.getGroup.add(new GroupListener[ActiveMQNode]() {
                def groupEvent(group: Group[ActiveMQNode], event: GroupEvent) {
                  if (event.equals(GroupEvent.CONNECTED) || event.equals(GroupEvent.CHANGED)) {
                    if (discoveryAgent.getGroup.isMaster(name)) {
                      if (started.compareAndSet(false, true)) {
                        if (take_pool(ClusteredConfiguration.this)) {
                          info("Broker %s is now the master, starting the broker.", name)
                          start()
                        } else {
                          update_pool_state()
                          started.set(false)
                        }
                      }
                    } else {
                      if (started.compareAndSet(true, false)) {
                        return_pool(ClusteredConfiguration.this)
                        info("Broker %s is now a slave, stopping the broker.", name)
                        stop()
                      } else {
                        if (event.equals(GroupEvent.CHANGED)) {
                          info("Broker %s is slave", name)
                          discoveryAgent.setServices(Array[String]())
                        }
                      }
                    }
                  } else if (event.equals(GroupEvent.DISCONNECTED)) {
                    info("Disconnected from the group", name)
                    discoveryAgent.setServices(Array[String]())
                    pool_enabled = false
                  }
                }
              })
              info("Broker %s is waiting to become the master", name)
              update_pool_state()
            }
          }
        }
      }
    }

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
        start()
      }
    } else {
      urlHandlerService.waitForService(60000L)
      updateCurator(curator)
    }

    def close() = {
      this.synchronized {
        if( pool_enabled ) {
          return_pool(ClusteredConfiguration.this)
        }
        if( discoveryAgent!=null ) {
          discoveryAgent.stop()
        }
        if(started.compareAndSet(true, false)) {
          stop()
        }
      }
      waitForStop()
      executor.shutdownNow()
    }

    def osgiRegister(broker: BrokerService): Unit = {
      val connectionFactory = new ActiveMQConnectionFactory("vm://" + broker.getBrokerName + "?create=false")
      cfServiceRegistration = bundleContext.registerService(classOf[javax.jms.ConnectionFactory].getName, connectionFactory, mutable.HashMap("name" -> broker.getBrokerName))
      debug("registerService of type " + classOf[javax.jms.ConnectionFactory].getName  + " as: " + connectionFactory + " with name: " + broker.getBrokerName + "; " + cfServiceRegistration)
    }

    def osgiUnregister(broker: BrokerService): Unit = {
      if (cfServiceRegistration != null) cfServiceRegistration.unregister()
      debug("unregister connection factory for: " + broker.getBrokerName + "; " + cfServiceRegistration)
    }

    def start() = this.synchronized {
      if (start_future == null || start_future.isDone) {
        info("Broker %s is being started.", name)
        start_future = executor.submit(new Runnable() {
          override def run() {
            doStart()
          }
        })
      }
    }

    def stop() = this.synchronized {
      if (stop_future == null || stop_future.isDone) {
        interruptAndWaitForStart()
        stop_future = executor.submit(new Runnable() {
          override def run() {
            doStop()
          }
        })
      }
    }

    private def doStart() {
      var start_failure:Throwable = null
      try {
        // If we are in a fabric, let pass along the zk password in the props.
        val fs = fabricService.getService
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
        info("booting up a broker from: " + config)
        server = createBroker(config, properties)
        // configure ports
        server._2.getTransportConnectors.foreach {
          t => {
            val portKey = t.getName + "-port"
            if (properties.containsKey(portKey)) {
              val template = t.getUri
              t.setUri(new URI(template.getScheme, template.getUserInfo, template.getHost,
                Integer.valueOf("" + properties.get(portKey)),
                template.getPath, template.getQuery, template.getFragment))
            }
          }
        }
        server._2.start()
        info("Broker %s has started.", name)

        server._2.waitUntilStarted
        server._2.addShutdownHook(new Runnable(){
          def run():Unit = {
            // Start up the server again if it shutdown.  Perhaps
            // it has lost a Locker and wants a restart.
            if(started.get && server!=null && server._2.isRestartAllowed && server._2.isRestartRequested){
              info("restarting after shutdown on restart request")
              start()
            }
          }
        })

        // Update the advertised endpoint URIs that clients can use.
        if (!standalone || replicating) {
          discoveryAgent.setServices( connectors.flatMap { name=>
            val connector = server._2.getConnectorByName(name)
            if ( connector==null ) {
              warn("ActiveMQ broker '%s' does not have a connector called '%s'", name, name)
              None
            } else {
              Some(connector.getConnectUri.getScheme + "://${zk:" + System.getProperty("karaf.name") + "/ip}:" + connector.getPublishableConnectURI.getPort)
            }
          })
        }

        if (registerService) osgiRegister(server._2)
      } catch {
        case e:Throwable =>
          info("Broker %s failed to start.  Will try again in 10 seconds", name)
          LOG.error("Exception on start: " + e, e)
          try {
            Thread.sleep(1000 * 10)
          } catch {
            case ignore:InterruptedException =>
          }
          start_failure = e
      } finally {
        if(started.get && start_failure!=null){
          start()
        } else {
          if (server!=null && server._3!=null)
            last_modified = server._3.lastModified()
        }
      }
    }

    private def doStop() {
      val s = server // working with a volatile
      if( s!=null ) {
        try {
          s._2.stop()
          s._2.waitUntilStopped()
          if (!standalone || replicating) {
            // clear out the services as we are no longer alive
            discoveryAgent.setServices( Array[String]() )
          }
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
        server = null
      }
    }

    private def interruptAndWaitForStart() {
      if (start_future != null && !start_future.isDone) {
        start_future.cancel(true)
      }
    }

    private def waitForStop() {
      if (stop_future != null && !stop_future.isDone) {
        stop_future.get()
      }
    }

  }


  ////////////////////////////////////////////////////////////////////////////
  // Maintain a registry of configuration based on ManagedServiceFactory events.
  ////////////////////////////////////////////////////////////////////////////
  val configurations = new mutable.HashMap[String, ClusteredConfiguration]

  class ConfigThread extends Thread {

    var running = true

    override def run() {
      while (running) {
        configurations.values.foreach(c => {
          try {
            if (c.config_check && c.last_modified != -1 && c.server != null) {
              val lm = c.server._3.lastModified()
              if( lm != c.last_modified ) {
                c.last_modified = lm
                info("updating " + c.properties)
                updated(c.properties.get("service.pid").asInstanceOf[String], c.properties.asInstanceOf[Dictionary[java.lang.String, _]])
              }
            }
          } catch {
            case t: Throwable => {}
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

  def updated(pid: String, properties: Dictionary[java.lang.String, _]): Unit = this.synchronized {
    try {
      deleted(pid)
      configurations.put(pid, ClusteredConfiguration(properties))
    } catch {
      case e: Exception => throw new ConfigurationException(null, "Unable to parse ActiveMQ configuration: " + e.getMessage).initCause(e).asInstanceOf[ConfigurationException]
    }
  }

  def deleted(pid: String): Unit = this.synchronized {
    configurations.remove(pid).foreach(_.close())
  }

  def getName: String = "ActiveMQ Server Controller"

  val config_thread : ConfigThread = new ConfigThread
  config_thread.setName("ActiveMQ Configuration Watcher")
  config_thread.start()

  //
  // Curator and FabricService tracking
  //
  val fabricService = new ServiceTracker[FabricService,FabricService](bundleContext, classOf[FabricService], null)
  fabricService.open()

  var curator: CuratorFramework = _
  val boundCuratorRefs = new java.util.ArrayList[ServiceReference[CuratorFramework]]()
  val curatorService = new ServiceTracker[CuratorFramework,CuratorFramework](bundleContext, classOf[CuratorFramework], this)
  curatorService.open()
  // we need to make sure "profile" url handler is available
  val filter = FrameworkUtil.createFilter("(&(objectClass=org.osgi.service.url.URLStreamHandlerService)(url.handler.protocol=profile))")
  val urlHandlerService = new ServiceTracker[URLStreamHandlerService,URLStreamHandlerService](bundleContext, filter, null)
  urlHandlerService.open()


  def addingService(reference: ServiceReference[CuratorFramework]): CuratorFramework = {
    val curator = bundleContext.getService(reference)
    boundCuratorRefs.add( reference )
    java.util.Collections.sort( boundCuratorRefs )
    val bind = boundCuratorRefs.get( 0 )
    if( bind == reference )
      bindCurator( curator )
    else
      bindCurator( curatorService.getService( bind ) )
    curator
  }

  def modifiedService(reference: ServiceReference[CuratorFramework], service: CuratorFramework) = {
  }

  def removedService(reference: ServiceReference[CuratorFramework], service: CuratorFramework) = {
    boundCuratorRefs.remove( reference )
    if( boundCuratorRefs.isEmpty )
      bindCurator( null )
    else
      bindCurator( curatorService.getService( boundCuratorRefs.get( 0 ) ) )
  }

  def bindCurator( curator: CuratorFramework ) = {
    this.curator = curator
    ActiveMQServiceFactory.this.synchronized {
      configurations.values.foreach { c=>
        c.updateCurator(curator)
      }
    }
  }

  //
  // Lifecycle
  //

  def destroy(): Unit = this.synchronized {
    config_thread.running = false
    config_thread.interrupt()
    config_thread.join()
    configurations.keys.toArray.foreach(deleted)
    fabricService.close()
    curatorService.close()
    urlHandlerService.close()
  }


}
