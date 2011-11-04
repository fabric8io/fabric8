/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.activemq

import org.osgi.framework.BundleContext
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedServiceFactory
import org.slf4j.LoggerFactory
import reflect.BeanProperty
import org.linkedin.zookeeper.client.IZKClient
import java.util.{Properties, Dictionary}
import collection.mutable.HashMap
import java.util.concurrent.atomic.AtomicBoolean
import org.apache.activemq.broker.BrokerService
import org.springframework.core.io.Resource
import org.apache.activemq.spring.Utils
import org.apache.xbean.spring.context.ResourceXmlApplicationContext
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader
import java.beans.PropertyEditorManager
import java.net.URI
import org.apache.xbean.spring.context.impl.URIEditor
import org.springframework.beans.factory.FactoryBean
import org.fusesource.fabric.groups.ChangeListener
import java.lang.{Thread, ThreadLocal}

object ActiveMQServiceFactory {
  final val LOG= LoggerFactory.getLogger(classOf[ActiveMQServiceFactory])
  final val CONFIG_PROPERTIES = new ThreadLocal[Properties]()

  PropertyEditorManager.registerEditor(classOf[URI], classOf[URIEditor])

  def info(str: String, args: AnyRef*) = if (LOG.isInfoEnabled) {
    println(String.format(str, args:_*))
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
      (ctx, broker)
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
  var zooKeeper: IZKClient = null

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
              c.update_pool_state
            }
          }
        }
      }
    }.start
  }
  

  case class ClusteredConfiguration(properties:Properties) {

    val name = Option(properties.getProperty("name")).getOrElse(arg_error("name property must be set"))
    val config = Option(properties.getProperty("config")).getOrElse(arg_error("config property must be set"))
    val group = Option(properties.getProperty("group")).getOrElse("default")
    val pool = Option(properties.getProperty("standby.pool")).getOrElse(null)
    val connectors = Option(properties.getProperty("connectors")).getOrElse("").split("""\s""")

    val started = new AtomicBoolean
    info("Broker %s is waiting to become the master", name)

    val discoveryAgent = new FabricDiscoveryAgent
    discoveryAgent.setAgent(System.getProperty("karaf.name"))
    discoveryAgent.setId(name)
    discoveryAgent.setGroupName(group)
    discoveryAgent.setZkClient(zooKeeper)
    discoveryAgent.singleton.add(new ChangeListener(){
      def changed {
        if( discoveryAgent.singleton.isMaster ) {
          if(started.compareAndSet(false, true)) {
            if( take_pool(ClusteredConfiguration.this) ) {
              info("Broker %s is now the master, starting the broker.", name)
              start
            } else {
              update_pool_state
              started.set(false)
            }
          }
        } else {
          if(started.compareAndSet(true, false)) {
            return_pool(ClusteredConfiguration.this)
            info("Broker %s is now a slave, stopping the broker.", name)
            stop
          }          
        }
      }
      def connected = changed
      def disconnected = changed
    })
    
    var pool_enabled = false
    update_pool_state
    
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
    

    @volatile
    var start_thread:Thread = _
    @volatile
    var server:(ResourceXmlApplicationContext, BrokerService) = _

    def close = this.synchronized {
      if( pool_enabled ) {
        discoveryAgent.stop()
      }
      if(started.compareAndSet(true, false)) {
        stop
      }
    }

    def start = {
      // Startup async so that we do not block the ZK event thread.
      def trystartup:Unit = {
        start_thread = new Thread("Startup for ActiveMQ Broker: "+name) {
          override def run() {
            var start_failure:Throwable = null
            try {
              // ok boot up the server..
              server = createBroker(config, properties)
              server._2.start()
              info("Broker %s has started.", name)

              // Update the advertised endpoint URIs that clients can use.
              discoveryAgent.setServices( connectors.flatMap { name=>
                val connector = server._2.getConnectorByName(name)
                if ( connector==null ) {
                  warn("ActiveMQ broker '%s' does not have a connector called '%s'", name, name)
                  None
                } else {
                  Some(connector.getPublishableConnectString)
                }
              })

            } catch {
              case e:Throwable =>
                info("Broker %s failed to start.  Will try again in 10 seconds", name)
                e.printStackTrace()
                Thread.sleep(1000*10);
                start_failure = e
            } finally {
              if(started.get && start_failure!=null){
                trystartup
              } else {
                start_thread = null
              }
            }
          }
        }
        start_thread.start()
      }
      trystartup
    }
    
    def stop = {
      info("Broker %s is being stoped.", name)
      new Thread("Startup for ActiveMQ Broker: "+name) {
        override def run() {
          var t = start_thread // working with a volatile
          while(t!=null) {
            t.interrupt()
            t.join()
            t = start_thread // when the start up thread gives up trying to start this gets set to null.
          }

          val s = server // working with a volatile
          if( s!=null ) {
            try {
              s._2.stop()
            } catch {
              case e:Throwable => e.printStackTrace()
            }
            try {
              s._1.close()
            } catch {
              case e:Throwable => e.printStackTrace()
            }
            try {
              discoveryAgent.stop()
            } catch {
              case e:Throwable => e.printStackTrace()
            }
            server = null
          }
        }
      }.start
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  // Maintain a registry of configuration based on ManagedServiceFactory events.
  ////////////////////////////////////////////////////////////////////////////
  val configurations = new HashMap[String, ClusteredConfiguration]

  def updated(pid: String, properties: Dictionary[_, _]): Unit = this.synchronized {
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
  }

  def getName: String = {
    return "ActiveMQ Server Controller"
  }

}